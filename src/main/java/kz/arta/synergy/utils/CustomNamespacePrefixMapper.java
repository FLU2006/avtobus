package kz.arta.synergy.utils;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class CustomNamespacePrefixMapper extends NamespacePrefixMapper {
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        switch (namespaceUri) {
            case "urn:EEC:M:ComplexDataObjects:v0.4.15":
            case "urn:EEC:M:ComplexDataObjects:v0.4.5":
                return "ccdo";
            case "urn:EEC:M:TR:ComplexDataObjects:v1.4.4":
            case "urn:EEC:M:TR:ComplexDataObjects:v1.0.3":
                return "trcdo";
            case "urn:EEC:M:SimpleDataObjects:v0.4.15":
            case "urn:EEC:M:SimpleDataObjects:v0.4.5":
                return "csdo";
            case "urn:EEC:M:TR:SimpleDataObjects:v1.4.4":
            case "urn:EEC:M:TR:SimpleDataObjects:v1.0.3":
                return "trsdo";
            case "urn:EEC:R:TR:TS:01:ConformityDocsRegistryDetails:v1.0.1":
            case "urn:EEC:R:ResourceStatusDetails:v0.4.5":
                return "doc";
            default:
                return suggestion;
        }
    }
}

