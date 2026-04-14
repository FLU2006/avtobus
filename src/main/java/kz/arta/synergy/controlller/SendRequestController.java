package kz.arta.synergy.controlller;

import kz.arta.synergy.data.generated.eec.r.resourcestatusdetails.v0_4.ResourceStatusDetailsType;
import kz.arta.synergy.data.generated.eec.r.tr.ts._01.conformitydocsregistrydetails.v1_0.ConformityDocsRegistryDetailsType;
import kz.arta.synergy.service.SendRequestToSHEP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@CrossOrigin(origins = "")
@Controller
@RequestMapping(value = "/ktrm-integration/api", produces = MediaType.APPLICATION_XML_VALUE)
public class SendRequestController {

    Logger log = LoggerFactory.getLogger(SendRequestController.class);


    @Autowired
    SendRequestToSHEP sendRequestToSHEP;

    @PostMapping(value = "/sendRequest", produces = "application/xml; charset=UTF-8")
    @ResponseBody
    public String sendRequest(@RequestParam String documentID, @RequestBody ConformityDocsRegistryDetailsType json) {
        try {
            return sendRequestToSHEP.sendARequestToSHEP(json, documentID);
        } catch (ClassNotFoundException | JAXBException | IOException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/sendResourceStatusDetails", produces = "application/xml; charset=UTF-8")
    @ResponseBody
    public String sendNotification(@RequestBody ResourceStatusDetailsType json) {
        try {
            return sendRequestToSHEP.sendResourceStatusDetails(json);
        } catch (ClassNotFoundException | JAXBException | IOException | DatatypeConfigurationException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/getResourceStatusDetails", produces = "application/xml; charset=UTF-8")
    @ResponseBody
    public String getMessage(@RequestBody ResourceStatusDetailsType json) {
        try {
            return sendRequestToSHEP.sendMessage(json);
        } catch (ClassNotFoundException | JAXBException | IOException | DatatypeConfigurationException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/xml-saver", produces = "application/xml; charset=UTF-8")
    @ResponseBody
    public String saveZipRequest(@RequestParam(required = false, defaultValue = "default") String folder, @RequestParam String documentID, @RequestBody ConformityDocsRegistryDetailsType json) {
        try {
            return sendRequestToSHEP.saveZipRequest(json, folder, documentID);
        } catch (ClassNotFoundException | IOException | DatatypeConfigurationException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
