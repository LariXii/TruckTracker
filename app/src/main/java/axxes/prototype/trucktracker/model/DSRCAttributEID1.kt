package axxes.prototype.trucktracker.model

enum class DSRCAttributEID1(val attribut: DSRCAttribut) {
    CONTEXT_MARK(
        DSRCAttribut(
            "Context mark",
            0,
            1,
            32,
            6,
            cacheable = false,
            temporaryData = false,
            data = listOf<Byte>(0x20, 0xf7.toByte(), 0x7f, 0xf0.toByte(), 0x00, 0x01, 0x00).toByteArray()
        )
    ),
    CONTRACT_AUTHENTICATOR(
        DSRCAttribut(
            "Contract Authenticator",
            4,
            1,
            36,
            1+4
        )
    ),
    RECEIPT_TEXT(
        DSRCAttribut(
            "Receipt text",
            12,
            1,
            44,
            1+20,
            true
        )
    ),
    RECEIPT_AUTHENTIFICATOR(
        DSRCAttribut(
            "Receipt authenticator",
            13,
            1,
            45,
            1+4,
            true
        )
    ),
    LICENSE_PLATE_NUMBER(
        DSRCAttribut(
            "License plate number",
            16,
            1,
            47,
            3+14
        )
    ),
    VEHICLE_CLASS(
        DSRCAttribut(
            "Vehicle class",
            17,
            1,
            49,
            1
        )
    ),
    VEHICLE_DIMENSIONS(
        DSRCAttribut(
            "Vehicle dimensions",
            18,
            1,
            50,
            3
        )
    ),
    VEHICLE_AXLES(
        DSRCAttribut(
            "Vehicle axles",
            19,
            1,
            51,
            2
        )
    ),
    VEHICLE_WEIGHT_LIMITS(
        DSRCAttribut(
            "Vehicle weight limits",
            20,
            1,
            52,
            6
        )
    ),
    VEHICLE_SPECIFIC_CHARACTERISTICS(
        DSRCAttribut(
            "Vehicle specific characteristics",
            22,
            1,
            54,
            4
        )
    ),
    VEHICLE_AUTHENTICATOR(
        DSRCAttribut(
            "Vehicle authenticator",
            23,
            1,
            55,
            1+4
        )
    ),
    EQUIPMENT_OBU_ID(
        DSRCAttribut(
            "Equipment OBU ID",
            24,
            1,
            56,
            1+4
        )
    ),
    EQUIPMENT_STATUS(
        DSRCAttribut(
            "Equipment status",
            26,
            1,
            58,
            2,
            true
        )
    ),
    PAYMENT_MEANS(
        DSRCAttribut(
            "Payment means",
            32,
            1,
            64,
            14
        )
    ),
    RECEIPT_DATA_1(
        DSRCAttribut(
            "Receipt data 1",
            33,
            1,
            65,
            28,
            true
        )
    ),
    RECEIPT_DATA_2(
        DSRCAttribut(
            "Receipt data 2",
            34,
            1,
            65,
            28,
            true
        )
    ),
    DANGEROUS_GOODS_1(
        DSRCAttribut(
            "Dangerous goods 1",
            95,
            1,
            2,
            1+20
        )
    ),
    DANGEROUS_GOODS_2(
        DSRCAttribut(
            "Dangerous goods 2",
            96,
            1,
            2,
            1+20
        )
    ),
    AUTHENTICATION_KEY_1(
        DSRCAttribut(
            "Authentication Key 1",
            111,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_2(
        DSRCAttribut(
            "Authentication Key 2",
            112,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_3(
        DSRCAttribut(
            "Authentication Key 3",
            113,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_4(
        DSRCAttribut(
            "Authentication Key 4",
            114,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_5(
        DSRCAttribut(
            "Authentication Key 5",
            115,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_6(
        DSRCAttribut(
            "Authentication Key 6",
            116,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_7(
        DSRCAttribut(
            "Authentication Key 7",
            117,
            1,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_8(
        DSRCAttribut(
            "Authentication Key 8",
            118,
            1,
            2,
            1+8
        )
    ),
    OBU_ID_TIS_D_PASS(
        DSRCAttribut(
            "OBU ID TIS: D-PASS",
            124,
            1,
            2,
            1+40,
            true
        )
    ),
    OBU_ID_TIS_D_EVE(
        DSRCAttribut(
            "OBU ID TIS: D-EVE",
            125,
            1,
            2,
            1+6
        )
    ),
    VST_DATA(
        DSRCAttribut(
            "VST Data",
            127,
            1,
            2,
            1+8,
            cacheable = false,
            temporaryData = false,
            data = listOf<Byte>(0x01, 0x00).toByteArray()
        )
    )
}