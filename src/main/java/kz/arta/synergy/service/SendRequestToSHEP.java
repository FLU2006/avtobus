package kz.arta.synergy.service;
import kz.arta.synergy.data.generated.eec.r.resourcestatusdetails.v0_4.ResourceStatusDetailsType;
import kz.arta.synergy.data.generated.eec.r.tr.ts._01.conformitydocsregistrydetails.v1_0.ConformityDocsRegistryDetailsType;
import org.springframework.stereotype.Component;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@Component
public interface SendRequestToSHEP {

    String jsonToXml(ConformityDocsRegistryDetailsType json) throws ClassNotFoundException, IOException, JAXBException;

    <T> String jsonToXml(T json, Class<T> clazz) throws ClassNotFoundException, IOException, JAXBException;

    String sendRequestToSHEP(String requestXml, String documentId);

    String sendARequestToSHEP(ConformityDocsRegistryDetailsType json, String documentID) throws JAXBException, IOException, ClassNotFoundException;

    String sendResourceStatusDetails(ResourceStatusDetailsType json) throws JAXBException, IOException, ClassNotFoundException, DatatypeConfigurationException;

    String sendMessage(ResourceStatusDetailsType json) throws JAXBException, IOException, ClassNotFoundException, DatatypeConfigurationException;

    String saveZipRequest(ConformityDocsRegistryDetailsType json, String folder, String documentId) throws IOException, ClassNotFoundException, DatatypeConfigurationException;
}
