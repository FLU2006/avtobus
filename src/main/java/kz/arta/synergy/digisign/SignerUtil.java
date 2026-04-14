package kz.arta.synergy.digisign;

import kz.gov.pki.kalkan.asn1.pkcs.PKCSObjectIdentifiers;
import kz.gov.pki.kalkan.jce.provider.KalkanProvider;
import kz.gov.pki.kalkan.xmldsig.KncaXS;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.encryption.XMLCipherParameters;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.UUID;

public class SignerUtil {

    public static final String WSU_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    private static Provider provider;

    public static Logger log = LoggerFactory.getLogger(SignerUtil.class);

    static {
        provider = new KalkanProvider();
        boolean exists = false;
        Provider[] providers = Security.getProviders();
        for (Provider p : providers) {
            if (p.getName().equals(provider.getName())) {
                exists = true;
            }
        }
        if (!exists) {
            Security.addProvider(provider);
            KncaXS.loadXMLSecurity();
        }

    }

    private synchronized static Certificate getCertificate(File keyPath, String keyPassword) throws Exception {
        try(InputStream inputStream = new FileInputStream(keyPath)) {
            KeyStore store = KeyStore.getInstance("PKCS12", provider.getName());
            store.load(inputStream, keyPassword.toCharArray());
            Enumeration<String> als = store.aliases();
            String alias = null;
            while (als.hasMoreElements()) {
                alias = als.nextElement();
            }

            final PrivateKey privateKey = (PrivateKey) store.getKey(alias, keyPassword.toCharArray());
            final X509Certificate x509Certificate = (X509Certificate) store.getCertificate(alias);
            SignerUtil signerUtil = new SignerUtil();
            Certificate certificate = signerUtil.new Certificate();
            certificate.setCert(x509Certificate);
            certificate.setPrivKey(privateKey);
            return certificate;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static String signXML(String xml, File keyPath, String keyPassword) throws Exception {

        Certificate certificate = getCertificate(keyPath, keyPassword);
        return signXML(xml, certificate.getCert(), certificate.getPrivKey());

    }

    public static String signXML(String xml, X509Certificate cert, PrivateKey privKey) throws Exception {
        return signXML(parseDocument(xml), cert, privKey);
    }

    public static String signXML(Document doc, X509Certificate cert, PrivateKey privKey) throws Exception {
        StringWriter os = null;
        String signMethod;
        String digestMethod;
        if ("RSA".equals(privKey.getAlgorithm())) {
            signMethod = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha1";
            digestMethod = "http://www.w3.org/2001/04/xmldsig-more#sha1";
        } else if("ECGOST3410-2015".equals(privKey.getAlgorithm())){
            signMethod = "urn:ietf:params:xml:ns:pkigovkz:xmlsec:algorithms:gostr34102015-gostr34112015-512";
            digestMethod = "urn:ietf:params:xml:ns:pkigovkz:xmlsec:algorithms:gostr34112015-512";
        } else {
            signMethod = "http://www.w3.org/2001/04/xmldsig-more#gost34310-gost34311";
            digestMethod = "http://www.w3.org/2001/04/xmldsig-more#gost34311";
        }
        XMLSignature sig = new XMLSignature(doc, "", signMethod);
        String res = "";
        try {
            if (doc.getFirstChild() != null) {
                doc.getFirstChild().appendChild(sig.getElement());
                Transforms transforms = new Transforms(doc);
                transforms.addTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
                transforms.addTransform("http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments");
                sig.addDocument("", transforms, digestMethod);
                sig.addKeyInfo(cert);
                sig.sign(privKey);
                os = new StringWriter();
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer trans = tf.newTransformer();
                trans.transform(new DOMSource(doc), new StreamResult(os));
                os.flush();
                res = os.toString();
                os.close();
            }
            return res;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static SOAPMessage getSoapMessageFromString(String xml) throws SOAPException, IOException {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        SOAPMessage sm = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage(new MimeHeaders(), is);
        return sm;
    }

    public static String signWSS(File container, String password, String xmlStr) throws SOAPException, IOException {

        final String signMethod;
        final String digestMethod;

        SOAPMessage msg = getSoapMessageFromString(xmlStr);
        if (msg.getSOAPHeader() == null) {
            msg.getSOAPPart().getEnvelope().addHeader();
        }
        InputStream inputStream = null;
        try {
            Class.forName("kz.gov.pki.kalkan.jce.X509Principal");
            SOAPEnvelope env = msg.getSOAPPart().getEnvelope();
            SOAPBody body = env.getBody();

            WSSecHeader secHeader = new WSSecHeader();
            secHeader.setMustUnderstand(false);
            secHeader.insertSecurityHeader(env.getOwnerDocument());
            String bodyId = "id-" + UUID.randomUUID().toString();
            String prefix = "wsu";
            for (int i = 0; i < secHeader.getSecurityHeader().getAttributes().getLength(); i++) {
                Attr attr = (Attr) secHeader.getSecurityHeader().getAttributes().item(i);
                if (WSU_NAMESPACE.equals(attr.getNamespaceURI())) {
                    prefix = attr.getName();
                }
            }
            body.addAttribute(new QName(WSU_NAMESPACE, "Id", prefix), bodyId);
            body.setIdAttributeNS(WSU_NAMESPACE, "Id", true);

            SOAPHeader header = env.getHeader();
            if (header == null) {
                header = env.addHeader();
            }

            KeyStore store = KeyStore.getInstance("PKCS12", KalkanProvider.PROVIDER_NAME);

            inputStream = AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                @Override
                public FileInputStream run() throws Exception {
                    FileInputStream fis = new FileInputStream(container);
                    return fis;
                }
            });
            store.load(inputStream, password.toCharArray());
            Enumeration<String> als = store.aliases();
            String alias = null;
            while (als.hasMoreElements()) {
                alias = als.nextElement();
            }
            final PrivateKey privateKey = (PrivateKey) store.getKey(alias, password.toCharArray());
            final X509Certificate x509Certificate = (X509Certificate) store.getCertificate(alias);
            String sigAlgOid = x509Certificate.getSigAlgOID();
            if (sigAlgOid.equals(PKCSObjectIdentifiers.sha1WithRSAEncryption.getId())) {
                signMethod = Constants.MoreAlgorithmsSpecNS + "rsa-sha1";
                digestMethod = Constants.MoreAlgorithmsSpecNS + "sha1";
            } else if (sigAlgOid.equals(PKCSObjectIdentifiers.sha256WithRSAEncryption.getId())) {
                signMethod = Constants.MoreAlgorithmsSpecNS + "rsa-sha256";
                digestMethod = XMLCipherParameters.SHA256;
            } else if("ECGOST3410-2015".equals(privateKey.getAlgorithm())){
                signMethod = "urn:ietf:params:xml:ns:pkigovkz:xmlsec:algorithms:gostr34102015-gostr34112015-512";
                digestMethod = "urn:ietf:params:xml:ns:pkigovkz:xmlsec:algorithms:gostr34112015-512";
            } else {
                signMethod = Constants.MoreAlgorithmsSpecNS + "gost34310-gost34311";
                digestMethod = Constants.MoreAlgorithmsSpecNS + "gost34311";
            }

            Document doc = env.getOwnerDocument();

            Transforms transforms = new Transforms(env.getOwnerDocument());

            transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

            XMLSignature sig = new XMLSignature(env.getOwnerDocument(), "", signMethod,
                    Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            sig.addDocument("#" + bodyId, transforms, digestMethod);
            sig.getSignedInfo().getSignatureMethodElement().setNodeValue(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
            secHeader.getSecurityHeader().appendChild(sig.getElement());
            header.appendChild(secHeader.getSecurityHeader());

            SecurityTokenReference reference = new SecurityTokenReference(doc);
            reference.setKeyIdentifier(x509Certificate);
            sig.getKeyInfo().addUnknownElement(reference.getElement());

            sig.sign(privateKey);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            msg.writeTo(out);
            String res = new String(out.toByteArray(), "UTF-8");
            out.close();
            return res;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null)
                inputStream.close();
        }

    }


    private static Document parseDocument(String xml) throws IOException, ParserConfigurationException, SAXException {
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        return documentBuilder.parse(bais);
    }

    public class Certificate {
        private X509Certificate cert;
        private PrivateKey privKey;
        public X509Certificate getCert() {
            return cert;
        }
        public void setCert(X509Certificate cert) {
            this.cert = cert;
        }
        public PrivateKey getPrivKey() {
            return privKey;
        }
        public void setPrivKey(PrivateKey privKey) {
            this.privKey = privKey;
        }


    }
}

