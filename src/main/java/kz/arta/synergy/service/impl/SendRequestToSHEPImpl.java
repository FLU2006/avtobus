package kz.arta.synergy.service.impl;

import kz.arta.synergy.data.generated.eec.r.resourcestatusdetails.v0_4.ResourceStatusDetailsType;
import kz.arta.synergy.data.generated.eec.r.tr.ts._01.conformitydocsregistrydetails.v1_0.ConformityDocsRegistryDetailsType;
import kz.arta.synergy.digisign.SignerUtil;
import kz.arta.synergy.service.DocumentValidationService;
import kz.arta.synergy.service.FileStorageService;
import kz.arta.synergy.service.RestService;
import kz.arta.synergy.service.SendRequestToSHEP;
import kz.arta.synergy.utils.CustomNamespacePrefixMapper;
import kz.arta.synergy.utils.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.bind.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.xml.sax.SAXException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

@Service
public class SendRequestToSHEPImpl implements SendRequestToSHEP {
    @Value("${login}")
    private String senderIdSHEP;
    @Value("${password}")
    private String senderPassSHEP;
    @Value("${service_id}")
    private String serviceIdSHEP;
    @Value("${service_url}")
    private String urlSHEP;

    @Autowired
    private RestService restService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentValidationService documentValidationService;

    @Value("${keyFile}")
    private String keyFile;
    @Value("${keyPassword}")
    private String keyPassword;


    Logger log = LoggerFactory.getLogger(SendRequestToSHEPImpl.class);
    private static final String NAMESPACE = "receiver-service";


    @Override
    public String sendRequestToSHEP(String requestXml, String documentID) {

        String request;
        try {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());
            XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);

            String template = loadTemplate("/templates/template.xml");
            String zipRequest = createZipRequest(requestXml, documentID);

            File signDataFile = new File(keyFile);
            File signWSSFile = new File(keyFile);
            log.info("KEYFILE {}", keyFile);
            log.info("************ ZipRequest: \n{}\n ", zipRequest);
            log.info("************ end of ZipRequest******");
            String encoded = zipAndEncode("request.xml", zipRequest);

            String messageID = UUID.randomUUID().toString();
            log.info("messageID: {}", messageID);

            request = String.format(template, messageID, serviceIdSHEP, xmlCalendar.toString(),
                    senderIdSHEP, senderPassSHEP,
                    "105", messageID, "eec",
                    "1", xmlCalendar, "P.TS.01/P.TS.01.PRC.001/P.TS.01.TRN.001/P.TS.01.MSG.001",
                    UUID.randomUUID(), "1", "1", encoded);

            request = SignerUtil.signWSS(signWSSFile, keyPassword, request); //sign header
            log.info("Request: {}", request);
            return restService.getPostWithsSoapHeaders(urlSHEP, request);

        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage(), e);
            e.printStackTrace();
            return e.getMessage();
        }

    }

    String createZipRequest(String requestXml, String documentId) throws DatatypeConfigurationException, IOException {
        GregorianCalendar calendar = new GregorianCalendar();

        XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);

        String zipTemplate = loadTemplate("/templates/ziptemplate.xml");

//        String zipMessageId = UUID.randomUUID().toString();
        String procedureID = UUID.randomUUID().toString();
//        String conversationID = UUID.randomUUID().toString();
        String trackID = UUID.randomUUID().toString();

        String xml = String.format(zipTemplate, documentId, procedureID, documentId,
                trackID, xmlCalendar.toString(), requestXml);

        return fixXml(xml);

    }

    String createSourceStatusDetails(String requestXml) throws DatatypeConfigurationException, IOException {
        GregorianCalendar calendar = new GregorianCalendar();

        XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);

        String messageTemplate = loadTemplate("/templates/pt01.xml");

        String messageId = UUID.randomUUID().toString();
        String procedureID = UUID.randomUUID().toString();
        String conversationID = UUID.randomUUID().toString();
        String trackID = UUID.randomUUID().toString();

        String xml = String.format(messageTemplate, messageId, conversationID, procedureID,
                trackID, xmlCalendar.toString(), requestXml);

        return fixXml(xml);

    }

    String saveXML(String docId, String xml, String folder) throws IOException {
        return fileStorageService.saveXmlData(docId, xml, folder);
    }


    @Override
    public String sendARequestToSHEP(ConformityDocsRegistryDetailsType json, String documentID) throws JAXBException, IOException, ClassNotFoundException {
        ValidationResult validationResult = documentValidationService.validate(json);
        if (!validationResult.isValid()) {
            return String.format("[%s] %s", validationResult.getErrorCode(), validationResult.getErrorMessage());

        }
        String requestXml = jsonToXml(json);

        if (requestXml.startsWith("<error>")) {
            return requestXml;
        }

        // Validate XML against XSD schema
        String xsdValidationError = validateXmlAgainstXsd(requestXml);
        if (xsdValidationError != null) {
            return "<error>XSD validation failed: " + xsdValidationError + "</error>";
        }

        if (json == null ||
                json.getConformityDocDetails() == null ||
                json.getConformityDocDetails().isEmpty() ||
                json.getConformityDocDetails().get(0) == null ||
                json.getConformityDocDetails().get(0).getDocId() == null ||
                json.getConformityDocDetails().get(0).getDocId().isEmpty()) {
            return "<error>DocId не найден в запросе</error>";
        }

        return sendRequestToSHEP(requestXml,  documentID);
    }

    @Override
    public String sendResourceStatusDetails(ResourceStatusDetailsType json) throws JAXBException, IOException, ClassNotFoundException, DatatypeConfigurationException {
        String requestXml = jsonToXml(json, ResourceStatusDetailsType.class);

        if (requestXml.startsWith("<error>")) {
            return requestXml;
        }

        return sendResourceStatusDetailsToSHEP(requestXml);
    }

    private String sendResourceStatusDetailsToSHEP(String requestXml){

        String request;
        try {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());
            XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);

            String template = loadTemplate("/templates/template.xml");
            String zipRequest = createSourceStatusDetails(requestXml);

            File signWSSFile = new File(keyFile);
            log.info("KEYFILE {}", keyFile);
            log.info("************ SourceStatusDetails: \n{}\n ", zipRequest);
            log.info("************ end of SourceStatusDetails ******");
            String encoded = zipAndEncode("request.xml", zipRequest);

            String messageID = UUID.randomUUID().toString();
            log.info("messageID: {}", messageID);

            request = String.format(template, messageID, serviceIdSHEP, xmlCalendar.toString(),
                    senderIdSHEP, senderPassSHEP,
                    "105", messageID, "eec",
                    "1", xmlCalendar, "P.TS.01/P.TS.01.PRC.001/P.TS.01.TRN.001/P.TS.01.MSG.001",
                    UUID.randomUUID(), "1", "1", encoded);

            request = SignerUtil.signWSS(signWSSFile, keyPassword, request); //sign header
            log.info("Request for ResourceStatusDetails: {}", request);
            return restService.getPostWithsSoapHeaders(urlSHEP, request);

        } catch (Exception e) {
            log.error("Ошибка: {}", e.getMessage(), e);
            e.printStackTrace();
            return e.getMessage();
        }

    }

    @Override
    public String sendMessage(ResourceStatusDetailsType json) throws JAXBException, IOException, ClassNotFoundException, DatatypeConfigurationException {
        String requestXml = jsonToXml(json, ResourceStatusDetailsType.class);

        if (requestXml.startsWith("<error>")) {
            return requestXml;
        }

        return createSourceStatusDetails(requestXml);


    }

    @Override
    public String saveZipRequest(ConformityDocsRegistryDetailsType json, String folder, String documentId) throws IOException, ClassNotFoundException, DatatypeConfigurationException {
        ValidationResult validationResult = documentValidationService.validate(json);
        if (!validationResult.isValid()) {
            return String.format("[%s] %s", validationResult.getErrorCode(), validationResult.getErrorMessage());

        }

        String requestXml = jsonToXml(json);
        if (requestXml.startsWith("<error>")) {
            return requestXml;
        }

        // Validate XML against XSD schema
        String xsdValidationError = validateXmlAgainstXsd(requestXml);
        if (xsdValidationError != null) {
            return "<error>XSD validation failed: " + xsdValidationError + "</error>";
        }

        if (json == null ||
                json.getConformityDocDetails() == null ||
                json.getConformityDocDetails().isEmpty() ||
                json.getConformityDocDetails().get(0) == null ||
                json.getConformityDocDetails().get(0).getDocId() == null ||
                json.getConformityDocDetails().get(0).getDocId().isEmpty()) {
            return "<error>DocId не найден в запросе</error>";
        }

        String docID = json.getConformityDocDetails().get(0).getDocId();
        String xml = createZipRequest(requestXml, documentId);

        return saveXML(docID, xml, folder);
    }

    private String fixXml(String xmlString) {
        int soapEnvelopeEnd = xmlString.indexOf("</soap:Envelope>");
        if (soapEnvelopeEnd == -1) {
            return xmlString;
        }
        String cleanXml = xmlString.substring(0, soapEnvelopeEnd + "</soap:Envelope>".length());
        return cleanXml.trim();
    }


    public static String zipAndEncode(String fileName, String xmlContent) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(byteStream)) {
            ZipEntry entry = new ZipEntry(fileName);
            zipOut.putNextEntry(entry);
            zipOut.write(xmlContent.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
        byte[] zippedBytes = byteStream.toByteArray();
        return Base64.getEncoder().encodeToString(zippedBytes);
    }

    //for test
    public static String decodeAndUnzip(String base64Data) throws Exception {
        // Декодируем Base64 в массив байт
        byte[] zippedBytes = Base64.getDecoder().decode(base64Data);

        // Поток для чтения ZIP-архива
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(zippedBytes);
             ZipInputStream zipIn = new ZipInputStream(byteStream)) {

            ZipEntry entry = zipIn.getNextEntry();
            if (entry == null) {
                throw new IllegalArgumentException("ZIP archive is empty");
            }

            // Читаем содержимое файла внутри ZIP
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zipIn.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            zipIn.closeEntry();

            return out.toString(String.valueOf(StandardCharsets.UTF_8));
        }
    }


    @Override
    public String jsonToXml(ConformityDocsRegistryDetailsType jsonString) throws ClassNotFoundException, IOException {
        return jsonToXml(jsonString, ConformityDocsRegistryDetailsType.class);
    }

    @Override
    public <T> String jsonToXml(T jsonString, Class<T> clazz) throws ClassNotFoundException, IOException {
        String requestXml = convertToXml(jsonString, clazz);
        log.info("requestXml {}", requestXml);
        if (requestXml.startsWith("<error>")) {
            return requestXml;
        }
        requestXml = requestXml.replaceFirst("<\\?xml\\s+version=\"[^\"]*\"\\s+encoding=\"[^\"]*\"\\s+standalone=\"(yes|no)\"\\s*\\?>", "");

        return requestXml;
    }


    private String loadTemplate(String templatePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IOException("Шаблон не найден");
            }
            try (Scanner scanner = new Scanner(is, "UTF-8")) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private String convertToXml(Object data, Class<?> clazz) {
        try{
            JAXBContext context = JAXBContext.newInstance(clazz);

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new CustomNamespacePrefixMapper());


            StringWriter writer = new StringWriter();
            marshaller.marshal(data, writer);
            return writer.toString();
        } catch (JAXBException e) {
            return "<error>Ошибка при формировании XML: " + e.getMessage() + "</error>";
        }

    }

    /**
     * Validates XML string against XSD schemas from resources/xsd
     * @param xmlString XML content to validate
     * @return null if validation is successful, error message otherwise
     */
    private String validateXmlAgainstXsd(String xmlString) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

            // Load main XSD schema
            InputStream xsdStream = getClass().getResourceAsStream("/xsd/EEC_R_TR_TS_01_ConformityDocsRegistryDetails_v1.0.1.xsd");
            if (xsdStream == null) {
                return "Main XSD schema file not found";
            }

            schemaFactory.setResourceResolver(new LSResourceResolver() {
                @Override
                public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                    try {
                        String xsdPath = "/xsd/" + systemId;
                        InputStream stream = getClass().getResourceAsStream(xsdPath);
                        if (stream != null) {
                            return new LSInputImpl(publicId, systemId, stream);
                        }
                        log.warn("Could not find imported XSD: {}", xsdPath);
                    } catch (Exception e) {
                        log.error("Error loading XSD import: {}", systemId, e);
                    }
                    return null;
                }
            });

            Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();

            validator.validate(new StreamSource(new StringReader(xmlString)));

            log.info("XML validation against XSD successful");
            return null;

        } catch (SAXException e) {
            log.error("XSD validation error: {}", e.getMessage());
            return e.getMessage();
        } catch (IOException e) {
            log.error("IO error during XSD validation: {}", e.getMessage());
            return "IO error: " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error during XSD validation: {}", e.getMessage());
            return "Unexpected error: " + e.getMessage();
        }
    }

    /**
     * Implementation of LSInput for custom resource resolution
     */
    private static class LSInputImpl implements LSInput {
        private String publicId;
        private String systemId;
        private InputStream byteStream;

        public LSInputImpl(String publicId, String systemId, InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.byteStream = byteStream;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
        }

        @Override
        public InputStream getByteStream() {
            return byteStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
            this.byteStream = byteStream;
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }
    }

}
