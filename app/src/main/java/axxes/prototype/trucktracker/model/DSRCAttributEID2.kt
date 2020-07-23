package axxes.prototype.trucktracker.model

enum class DSRCAttributEID2 (val attribut: DSRCAttribut) {
    CONTEXT_MARK(
        DSRCAttribut(
            "Context mark",
            0,
            2,
            32,
            6,
            false,
            temporaryData = false,
            data = listOf<Byte>(0x20, 0xf7.toByte(), 0x7f, 0xf0.toByte(), 0x00, 0x02, 0x01).toByteArray()
        )
    ),
    LICENSE_PLATE_NUMBER(
        DSRCAttribut(
            "License plate number",
            16,
            2,
            47,
            3+14
        )
    ),
    VEHICLE_CLASS(
        DSRCAttribut(
            "Vehicle class",
            17,
            2,
            49,
            1
        )
    ),
    VEHICLE_DIMENSIONS(
        DSRCAttribut(
            "Vehicle dimensions",
            18,
            2,
            50,
            3
        )
    ),
    VEHICLE_AXLES(
        DSRCAttribut(
            "Vehicle axles",
            19,
            2,
            51,
            2
        )
    ),
    VEHICLE_WEIGHT_LIMITS(
        DSRCAttribut(
            "Vehicle weight limits",
            20,
            2,
            52,
            6
        )
    ),
    VEHICLE_SPECIFIC_CHARACTERISTICS(
        DSRCAttribut(
            "Vehicle specific characteristics",
            22,
            2,
            54,
            4
        )
    ),
    EQUIPMENT_OBU_ID(
        DSRCAttribut(
            "Equipment OBU ID",
            24,
            2,
            56,
            1+4
        )
    ),
    PAYMENT_MEANS(
        DSRCAttribut(
            "Payment means",
            32,
            2,
            64,
            14
        )
    ),
    TRAILER_CHARACTERISTICS(
        DSRCAttribut(
            "Trailer characteristics",
            46,
            2,
            79,
            5
        )
    ),
    VEHICLE_AXLES_HISTORY(
        DSRCAttribut(
            "Vehicle axles history",
            48,
            2,
            81,
            6
        )
    ),
    COMMUNICATION_STATUS(
        DSRCAttribut(
            "Communication status",
            49,
            2,
            82,
            8,
            true
        )
    ),
    GNSS_STATUS(
        DSRCAttribut(
            "GNSS status",
            50,
            2,
            83,
            23,
            true
        )
    ),
    DISTANCE_RECORDING_STATUS(
        DSRCAttribut(
            "Distance recording status",
            51,
            2,
            84,
            6,
            true
        )
    ),
    ACTIVE_CONTEXTS(
        DSRCAttribut(
            "Active contexts",
            52,
            2,
            85,
            1+4*14,
            true
        )
    ),
    OBE_STATUS_HISTORY(
        DSRCAttribut(
            "OBE status history",
            53,
            2,
            86,
            13
        )
    ),
    VEHICLE_WEIGHT_HISTORY(
        DSRCAttribut(
            "Vehicle weight history",
            55,
            2,
            88,
            14
        )
    ),
    EXTENDED_OBE_STATUS_HISTORY(
        DSRCAttribut(
            "Extended OBE status history",
            56,
            2,
            89,
            18
        )
    ),
    EXTENDED_VEHICLE_AXLES_HISTORY(
        DSRCAttribut(
            "Extended vehicle axles history",
            57,
            2,
            90,
            10
        )
    ),
    AUTHENTICATION_KEY_1(
        DSRCAttribut(
            "Authentication Key 1",
            111,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_2(
        DSRCAttribut(
            "Authentication Key 2",
            112,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_3(
        DSRCAttribut(
            "Authentication Key 3",
            113,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_4(
        DSRCAttribut(
            "Authentication Key 4",
            114,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_5(
        DSRCAttribut(
            "Authentication Key 5",
            115,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_6(
        DSRCAttribut(
            "Authentication Key 6",
            116,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_7(
        DSRCAttribut(
            "Authentication Key 7",
            117,
            2,
            2,
            1+8
        )
    ),
    AUTHENTICATION_KEY_8(
        DSRCAttribut(
            "Authentication Key 8",
            118,
            2,
            2,
            1+8
        )
    ),
    ELEMENT_ACCESS_KEY(
        DSRCAttribut(
            "Element access key",
            120,
            2,
            2,
            1+16
        )
    ),
    ACCR_KEYREF(
        DSRCAttribut(
            "AcCr KeyRef",
            121,
            2,
            2,
            1+2
        )
    ),
    VST_DATA(
        DSRCAttribut(
            "VST Data",
            127,
            2,
            2,
            1+8,
            cacheable = false,
            temporaryData = false,
            data = listOf<Byte>(0x03, 0x00, 0xf9.toByte(), 0xfa.toByte()).toByteArray()
        )
    )
}