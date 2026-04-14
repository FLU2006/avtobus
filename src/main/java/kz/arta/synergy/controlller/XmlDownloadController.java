package kz.arta.synergy.controlller;

import kz.arta.synergy.service.XmlDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@CrossOrigin(origins = "")
@Controller
@RequestMapping(value = "/ktrm-integration/api", produces = MediaType.APPLICATION_XML_VALUE)
public class XmlDownloadController {

    @Autowired
    XmlDownloadService xmlDownloadService;

    Logger log = LoggerFactory.getLogger(XmlDownloadController.class);

    @GetMapping("/download/xml")
    public void downloadLog(@RequestParam String filename, String folder, HttpServletResponse response) throws IOException {
        xmlDownloadService.downloadLog(filename, folder, response);
    }


    @GetMapping("/download/xmls")
    public void downloadFolder(HttpServletResponse response, String folder) throws IOException {
        xmlDownloadService.downloadFolder(response, folder);
    }



}
