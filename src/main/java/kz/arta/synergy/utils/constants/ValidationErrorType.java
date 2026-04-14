package kz.arta.synergy.utils.constants;

public enum ValidationErrorType {
    ADDRESS_CODE_EMPTY("ФЛК 3", "Поле AddressKindCode не заполнено"),
    ADDRESS_CODE_INVALID("ФЛК 4", "AddressKindCode должен содержать только следующие значения:\n" +
            "«1» – адрес регистрации\n«2» – фактический адрес"),
    CONFORMITY_AUTHORITY_ID_EMPTY("ФЛК 5", "Поле conformityAuthorityId не заполнено"),
    UNIFIED_COUNTRY_CODE_EMPTY("ФЛК 5", "Поле unifiedCountryCode не заполнено"),
    COMMUNICATION_CHANNEL_CODE_EMPTY("ФЛК 6", "Поле communicationChannelCode не заполнено"),
    COMMUNICATION_CHANNEL_CODE_INVALID("ФЛК 7", "CommunicationChannelCode должен содержать только следующие значения:\n" +
            "«AO» – адрес сайта в сети Интернет\n«TE» – телефон\n«EM» – электронная почта\n«FX» – факс\n"),
    COMMUNICATION_CHANNEL_NAME_NOT_EMPTY("ФЛК 8", "Поле CommunicationChannelName в составе сложного реквизита «Контактный реквизит»\n" +
            "(ccdo:CommunicationDetails) не заполняется"),
    END_DATE_TIME_NOT_EMPTY("ФЛК 9", "Поле EndDateTime в составе сложного реквизита «Технологические характеристики записи общего ресурса» (ccdo:ResourceItemStatusDetails) не заполняется"),
    DOC_ID_DUPLICATE("ФЛК 10", "Сообщение не должно содержать элементов ConformityDocDetails с дублирующимся значением DocId"),
    CONFORMITY_DOC_KIND_CODE_EMPTY("ФЛК 11", "Поле conformityDocKindCode не заполнено"),
    DOC_ID_EMPTY("ФЛК 12", "Поле docId не заполнено"),
    CONFORMITY_DOC_KIND_NAME_EMPTY("ФЛК 12", "Поле conformityDocKindName не заполнено"),
    DOC_ID_PATTERN_MISMATCH("ФЛК 12", "Номер документа должен начинаться с «ЕАЭС» (сертификат) или «ТС» (декларация)."),
    DOC_CREATION_DATE_NOT_EMPTY("ФЛК 13", "Поле DocCreationDate в составе сложного реквизита «Документ об оценке соответствия» (trcdo:ConformityDocDetails) не заполняется"),
    COMMODITY_CODE_EMPTY("ФЛК 14", "CommodityCode должен содержать не менее 1 значения в составе хотя бы одного из следующих реквизитов: «Продукт» (trcdo:ProductDetails)\n«Единица продукта» (trcdo:ProductInstanceDetails)"),
    DOC_STATUS_CODE_EMPTY("ФЛК 19", "Поле DocStatusCode не заполнено"),
    DOC_STATUS_CODE_INVALID("ФЛК 19", "DocStatusCode должен содержать только следующие значения:\n" +
            "«01» – действует;\n«02» – приостановлен;\n«03» – прекращен;\n«04» – продлен;\n«05» – возобновлен;\n«09» – архивный"),
    PRODUCT_INSTANCE_ID_EMPTY("ФЛК 24", "Поле productInstanceId не заполнено"),
    PRODUCT_INSTANCE_NOT_FOUND("ФЛК 24 DATA", "Отсутствуют данные ProductInstanceDetails"),
    TECHNICAL_REGULATION_OBJECT_DETAILS_NOT_FOUND("ФЛК 24 DATA", "Отсутствуют данные technicalRegulationObjectDetails"),
    KIND_NAME_MISSING("ФЛК  24 DATA", "Отсутствуют данные TechnicalRegulationObjectKindName"),

    ;


    private final String code;
    private final String message;

    ValidationErrorType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage(Object... args) {
        return args.length > 0 ? String.format(message, args) : message;
    }

}
