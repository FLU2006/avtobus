package kz.arta.synergy.service;

import kz.arta.synergy.data.generated.eec.m.complexdataobjects.v0_4.AddressDetailsV4Type;
import kz.arta.synergy.data.generated.eec.m.complexdataobjects.v0_4.CommunicationDetailsType;
import kz.arta.synergy.data.generated.eec.m.complexdataobjects.v0_4.SubjectAddressDetailsType;
import kz.arta.synergy.data.generated.eec.m.simpledataobjects.v0_4.UnifiedCountryCodeType;
import kz.arta.synergy.data.generated.eec.m.simpledataobjects.v0_4.UnifiedPhysicalMeasureType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ConformityDocDetailsType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ManufacturerDetailsType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ProductDetailsType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ProductInstanceDetailsType;
import kz.arta.synergy.data.generated.eec.r.tr.ts._01.conformitydocsregistrydetails.v1_0.ConformityDocsRegistryDetailsType;
import kz.arta.synergy.utils.constants.ValidationErrorType;
import kz.arta.synergy.utils.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

@Service
public class DocumentValidationService {
    private final Validators validators;

    Logger log = LoggerFactory.getLogger(DocumentValidationService.class);


    @Autowired
    public DocumentValidationService(Validators validators) {
        this.validators = validators;
    }

    //all validation
    public ValidationResult validate(ConformityDocsRegistryDetailsType doc) {
        ConformityDocDetailsType docDetails = doc.getConformityDocDetails().get(0);
        ValidationResult result;

        //ФЛК 1 - нужно следить за тем, чтобы сертификаты не дублировались

        // ФЛК 2 - поля validityPeriodDetails.startDateTime, updateDateTime - сами обновляются
//        ????

        // ФЛК 3-4: AddressKindCode
        result = validateAddressKindCodes(docDetails);
        if (!result.isValid()) return result;


//        флк 5
        result = validateConformityAuthorityId(docDetails);
        if (!result.isValid()) return result;

        result = validateUnifiedCountryCode(docDetails);
        if (!result.isValid()) return result;

        //ФЛК 6 И 7
        result = validateCommunicationChannelCode(docDetails);
        if (!result.isValid()) return result;

        //ФЛК 8
        result = validateCommunicationChannelName(docDetails);
        if (!result.isValid()) return result;

//        ФЛК 9
        result = validateEndDateTime(docDetails);
        if (!result.isValid()) return result;

//        ФЛК 10
        result = validateNoDocIdDuplicate(doc.getConformityDocDetails());
        if (!result.isValid()) return result;

//        ФЛК 11
        result = validateConformityDocKindCode(docDetails);
        if (!result.isValid()) return result;

//        ФЛК 12
        result = validateDocId(docDetails);
        if (!result.isValid()) return result;

//        ФЛК 13
        result = validateDocCreationDate(docDetails);
        if (!result.isValid()) return result;

//      ФЛК 14
        result = validateCommodityCode(docDetails);
        if (!result.isValid()) return result;




        
        //ФЛК 19
        result = validateDocStatusCode(docDetails);
        if (!result.isValid()) return result;



//        ФЛК 24
//        result = validateTechnicalRegulationObjectKindName(docDetails);
//        if (!result.isValid()) return result;

        return ValidationResult.success();
    }

    private ValidationResult validateAddressKindCodes(ConformityDocDetailsType docDetails) {
        //в нескольких местах

        for (SubjectAddressDetailsType address : docDetails.getApplicantDetails().getSubjectAddressDetails()) {
            ValidationResult result = validators.validateAddressKindCode(address.getAddressKindCode());
            if (!result.isValid()) return result;
        }

        for (AddressDetailsV4Type address : docDetails.getConformityAuthorityV2Details().getAddressV4Details()) {
            ValidationResult result = validators.validateAddressKindCode(address.getAddressKindCode());
            if (!result.isValid()) return result;
        }

        for (ManufacturerDetailsType manufacturerDetailsType : docDetails.getTechnicalRegulationObjectDetails().getManufacturerDetails()) {
            for (AddressDetailsV4Type address : manufacturerDetailsType.getAddressV4Details()) {
                ValidationResult result = validators.validateAddressKindCode(address.getAddressKindCode());
                if (!result.isValid()) return result;
            }
        }


        return ValidationResult.success();
    }

    public ValidationResult validateConformityAuthorityId(ConformityDocDetailsType docDetails) {
        String authorityId = docDetails.getConformityAuthorityV2Details().getConformityAuthorityId();
        return validators.validateConformityAuthorityId(authorityId);
    }

    public ValidationResult validateUnifiedCountryCode(ConformityDocDetailsType docDetails) {
        UnifiedCountryCodeType unifiedCountryCode = docDetails
                .getConformityAuthorityV2Details()
                .getUnifiedCountryCode();

        return validators.validateUnifiedCountryCode(unifiedCountryCode);
    }

    public ValidationResult validateCommunicationChannelCode(ConformityDocDetailsType docDetails) {
        for (CommunicationDetailsType detailsType : docDetails.getConformityAuthorityV2Details().getCommunicationDetails()) {
            ValidationResult result = validators.validateCommunicationChannelCode(detailsType.getCommunicationChannelCode());
            if (!result.isValid()) return result;
        }

        for (CommunicationDetailsType detailsType : docDetails.getApplicantDetails().getCommunicationDetails()) {
            ValidationResult result = validators.validateCommunicationChannelCode(detailsType.getCommunicationChannelCode());
            if (!result.isValid()) return result;
        }

        for (ManufacturerDetailsType detailsType : docDetails.getTechnicalRegulationObjectDetails().getManufacturerDetails()) {
            for (CommunicationDetailsType communicationDetailsType : detailsType.getCommunicationDetails()) {
                ValidationResult result = validators.validateCommunicationChannelCode(communicationDetailsType.getCommunicationChannelCode());
                if (!result.isValid()) return result;
            }
        }

        return ValidationResult.success();
    }

    public ValidationResult validateCommunicationChannelName(ConformityDocDetailsType docDetails) {
        for (CommunicationDetailsType detailsType : docDetails.getConformityAuthorityV2Details().getCommunicationDetails()) {
            ValidationResult result = validators.validateCommunicationChannelName(detailsType.getCommunicationChannelName());
            if (!result.isValid()) return result;
        }

        for (CommunicationDetailsType detailsType : docDetails.getApplicantDetails().getCommunicationDetails()) {
            ValidationResult result = validators.validateCommunicationChannelName(detailsType.getCommunicationChannelName());
            if (!result.isValid()) return result;
        }

        for (ManufacturerDetailsType detailsType : docDetails.getTechnicalRegulationObjectDetails().getManufacturerDetails()) {
            for (CommunicationDetailsType communicationDetailsType : detailsType.getCommunicationDetails()) {
                ValidationResult result = validators.validateCommunicationChannelName(communicationDetailsType.getCommunicationChannelName());
                if (!result.isValid()) return result;
            }
        }

        return ValidationResult.success();
    }

    public ValidationResult validateEndDateTime(ConformityDocDetailsType docDetails) {
        return validators.validateEndDateTime(docDetails.getResourceItemStatusDetails().getValidityPeriodDetails().getEndDateTime());
    }

    public ValidationResult validateTechnicalRegulationObjectKindName(ConformityDocDetailsType docDetails) {
        if (docDetails == null || docDetails.getTechnicalRegulationObjectDetails() == null) {
            return ValidationResult.error(ValidationErrorType.TECHNICAL_REGULATION_OBJECT_DETAILS_NOT_FOUND);
        }
        String kindName = docDetails.getTechnicalRegulationObjectDetails().getTechnicalRegulationObjectKindName();
        return validators.validateTechnicalRegulationObjectKindName(kindName, getFirstProductInstance(docDetails));
    }

    private ProductInstanceDetailsType getFirstProductInstance(ConformityDocDetailsType docDetails) {
        try {
            return docDetails.getTechnicalRegulationObjectDetails()
                    .getProductDetails().get(0)
                    .getProductInstanceDetails().get(0);
        } catch (Exception e) {
            return null; // если списки пустые или null
        }
    }

    public ValidationResult validateNoDocIdDuplicate(List<ConformityDocDetailsType> docDetailsTypes) {
        return validators.validateNoDocIdDuplicate(docDetailsTypes);

    }

    public ValidationResult validateConformityDocKindCode(ConformityDocDetailsType docDetails) {
        return validators.validateConformityDocKindCode(docDetails.getConformityDocKindCode());
    }

    public ValidationResult validateDocId(ConformityDocDetailsType docDetails) {
        return validators.validateDocId(docDetails.getDocId(), docDetails.getConformityDocKindName());
    }

    public ValidationResult validateDocCreationDate(ConformityDocDetailsType docDetails) {
        return validators.validateDocCreationDate(docDetails.getDocCreationDate());
    }

    public ValidationResult validateCommodityCode(ConformityDocDetailsType docDetails) {
        if (docDetails == null
                || docDetails.getTechnicalRegulationObjectDetails() == null
                || docDetails.getTechnicalRegulationObjectDetails().getProductDetails() == null) {
            return ValidationResult.error(ValidationErrorType.COMMODITY_CODE_EMPTY);
        }
        List<ProductDetailsType> productDetails = docDetails.getTechnicalRegulationObjectDetails().getProductDetails();
        for (ProductDetailsType detailsType : productDetails) {
            ValidationResult result = validators.validateCommodityCode(detailsType);
            if (result.isValid()) {
                return ValidationResult.success();
            }
        }

        return ValidationResult.error(ValidationErrorType.COMMODITY_CODE_EMPTY);
    }

    public ValidationResult validateDocStatusCode(ConformityDocDetailsType docDetails) {
        return validators.validateDocStausCode(docDetails.getDocStatusDetails().getDocStatusCode());
    }


}
