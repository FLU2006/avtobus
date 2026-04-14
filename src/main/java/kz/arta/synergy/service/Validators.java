package kz.arta.synergy.service;

import kz.arta.synergy.data.generated.eec.m.simpledataobjects.v0_4.UnifiedCountryCodeType;
import kz.arta.synergy.data.generated.eec.m.simpledataobjects.v0_4.UnifiedPhysicalMeasureType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ConformityDocDetailsType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ProductDetailsType;
import kz.arta.synergy.data.generated.eec.m.tr.complexdataobjects.v1_0.ProductInstanceDetailsType;
import kz.arta.synergy.utils.constants.ValidationErrorType;
import kz.arta.synergy.utils.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
public class Validators {
    private static final Set<String> VALID_ADDRESS_CODES =
            new HashSet<>(Arrays.asList("1", "2"));

    private static final Set<String> VALID_СOMMUNICATION_CHANNEL_CODE =
            new HashSet<>(Arrays.asList("AO", "TE", "EM", "FX"));

    private static final Set<String> VALID_DOC_STATUS_CODE =
            new HashSet<>(Arrays.asList("01", "02", "03", "04", "05", "09"));


    Logger log = LoggerFactory.getLogger(Validators.class);


    public ValidationResult validateAddressKindCode(String code) {
        if (code == null || code.isEmpty()) {
            return ValidationResult.error(ValidationErrorType.ADDRESS_CODE_EMPTY);
        }
        if (!VALID_ADDRESS_CODES.contains(code)) {
            return ValidationResult.error(ValidationErrorType.ADDRESS_CODE_INVALID);
        }
        return ValidationResult.success();
    }


    public ValidationResult validateTechnicalRegulationObjectKindName(String kindName, ProductInstanceDetailsType instanceDetails) {

        if (kindName == null) {
            return ValidationResult.error(ValidationErrorType.KIND_NAME_MISSING);
        }

        if (!kindName.equals("партия") && !kindName.equals("единичное изделие")) {
            return ValidationResult.success();
        }

        if (instanceDetails == null) {
            return ValidationResult.error(ValidationErrorType.PRODUCT_INSTANCE_NOT_FOUND);
        }

        String productInstanceId = instanceDetails.getProductInstanceId();

        if (kindName.equals("партия")) {
            if (productInstanceId == null || productInstanceId.isEmpty()) {
                UnifiedPhysicalMeasureType measure = instanceDetails.getUnifiedCommodityMeasure();

                if (measure != null && measure.getValue() != null) {
                    instanceDetails.setProductInstanceId(String.valueOf(measure.getValue()));
                    return ValidationResult.success();
                } else {
                    return ValidationResult.error(ValidationErrorType.PRODUCT_INSTANCE_ID_EMPTY);
                }
            }
        }

        if (kindName.equals("единичное изделие")) {
            if (productInstanceId == null || productInstanceId.isEmpty()) {
                return ValidationResult.error(ValidationErrorType.PRODUCT_INSTANCE_ID_EMPTY);
            }
        }

        return ValidationResult.success();
    }


    public ValidationResult validateConformityAuthorityId(String conformityAuthorityId) {
        if (conformityAuthorityId == null || conformityAuthorityId.isEmpty()) {
            return ValidationResult.error(ValidationErrorType.CONFORMITY_AUTHORITY_ID_EMPTY);
        }
        return ValidationResult.success();
    }


    public ValidationResult validateUnifiedCountryCode(UnifiedCountryCodeType unifiedCountryCode) {
        if (unifiedCountryCode == null
                || isNullOrEmpty(unifiedCountryCode.getCodeListId())
                || isNullOrEmpty(unifiedCountryCode.getValue())) {
            return ValidationResult.error(ValidationErrorType.UNIFIED_COUNTRY_CODE_EMPTY);
        }
        return ValidationResult.success();
    }

    public ValidationResult validateCommunicationChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isEmpty()) {
            return ValidationResult.error(ValidationErrorType.COMMUNICATION_CHANNEL_CODE_EMPTY);
        }
        if (!VALID_СOMMUNICATION_CHANNEL_CODE.contains(channelCode)) {
            return ValidationResult.error(ValidationErrorType.COMMUNICATION_CHANNEL_CODE_INVALID);
        }
        return ValidationResult.success();
    }

    public ValidationResult validateCommunicationChannelName(String channelName) {
        if (channelName != null && !channelName.trim().isEmpty()) {
            return ValidationResult.error(ValidationErrorType.COMMUNICATION_CHANNEL_NAME_NOT_EMPTY);
        }

        return ValidationResult.success();
    }

    public ValidationResult validateEndDateTime(XMLGregorianCalendar endDate) {
        if (endDate != null) {
            return ValidationResult.error(ValidationErrorType.END_DATE_TIME_NOT_EMPTY);
        }

        return ValidationResult.success();
    }

    public ValidationResult validateNoDocIdDuplicate(List<ConformityDocDetailsType> docDetailsTypes) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        for (ConformityDocDetailsType docDetailsType : docDetailsTypes) {
            String docId = docDetailsType.getDocId();
            if (docId != null && !seen.add(docId)) {
                duplicates.add(docId);
            }
        }

        if (!duplicates.isEmpty()) {
            log.info("Duplicate DocId: {}", duplicates);
            return ValidationResult.error(ValidationErrorType.DOC_ID_DUPLICATE);
        }

        return ValidationResult.success();

    }

    public ValidationResult validateConformityDocKindCode(String docKindCode) {
        if (docKindCode == null && docKindCode.trim().isEmpty()) {
            return ValidationResult.error(ValidationErrorType.CONFORMITY_DOC_KIND_CODE_EMPTY);
        }

        return ValidationResult.success();
    }


    private boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }


    public ValidationResult validateDocId(String docId, String conformityDocKindName) {
        if (docId == null || docId.trim().isEmpty()) {
            return ValidationResult.error(ValidationErrorType.DOC_ID_EMPTY);

        }

        if (conformityDocKindName == null) {
            return ValidationResult.error(ValidationErrorType.CONFORMITY_DOC_KIND_NAME_EMPTY);
        }

        String normalizedName = conformityDocKindName.toLowerCase();


        if (normalizedName.startsWith("декларация")) {
            if (!docId.matches("^ЕАЭС.+")) {
                return ValidationResult.error(ValidationErrorType.DOC_ID_PATTERN_MISMATCH);
            }
        } else if (normalizedName.startsWith("сертификат")) {
            if (!docId.matches("^ЕАЭС.+")) {
                return ValidationResult.error(ValidationErrorType.DOC_ID_PATTERN_MISMATCH);
            }
        }

        return ValidationResult.success();
    }

    public ValidationResult validateDocCreationDate(XMLGregorianCalendar docCreationDate) {
        if (docCreationDate != null) {
            return ValidationResult.error(ValidationErrorType.DOC_CREATION_DATE_NOT_EMPTY);
        }

        return ValidationResult.success();

    }

    public ValidationResult validateDocStausCode(String statusCode) {
        if (statusCode == null || statusCode.isEmpty()) {
            return ValidationResult.error(ValidationErrorType.DOC_STATUS_CODE_EMPTY);
        }
        if (!VALID_DOC_STATUS_CODE.contains(statusCode)) {
            return ValidationResult.error(ValidationErrorType.DOC_STATUS_CODE_INVALID);
        }
        return ValidationResult.success();

    }

    public ValidationResult validateCommodityCode(ProductDetailsType detailsType) {
        if (detailsType == null) {
            return ValidationResult.error(ValidationErrorType.COMMODITY_CODE_EMPTY);
        }

        if (detailsType.getCommodityCode() != null && !detailsType.getCommodityCode().isEmpty()) {
            return ValidationResult.success();
        }

        if (detailsType.getProductInstanceDetails() != null) {
            for (ProductInstanceDetailsType type : detailsType.getProductInstanceDetails()) {
                if (type.getCommodityCode() != null && !type.getCommodityCode().isEmpty()) {
                    return ValidationResult.success();
                }
            }
        }

        return ValidationResult.error(ValidationErrorType.COMMODITY_CODE_EMPTY);

    }
}
