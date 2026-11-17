package com.teczo.khatabill.util.validators

/**
 * GST state / UT codes. The English names are identifiers for lookups only; anything shown to the user
 * must come from strings.xml (en, kn, hi), which the business setup step adds.
 */
object GstStateCodes {
    private val names: Map<String, String> = mapOf(
        "01" to "Jammu and Kashmir",
        "02" to "Himachal Pradesh",
        "03" to "Punjab",
        "04" to "Chandigarh",
        "05" to "Uttarakhand",
        "06" to "Haryana",
        "07" to "Delhi",
        "08" to "Rajasthan",
        "09" to "Uttar Pradesh",
        "10" to "Bihar",
        "11" to "Sikkim",
        "12" to "Arunachal Pradesh",
        "13" to "Nagaland",
        "14" to "Manipur",
        "15" to "Mizoram",
        "16" to "Tripura",
        "17" to "Meghalaya",
        "18" to "Assam",
        "19" to "West Bengal",
        "20" to "Jharkhand",
        "21" to "Odisha",
        "22" to "Chhattisgarh",
        "23" to "Madhya Pradesh",
        "24" to "Gujarat",
        "26" to "Dadra and Nagar Haveli and Daman and Diu",
        "27" to "Maharashtra",
        "28" to "Andhra Pradesh (before division)",
        "29" to "Karnataka",
        "30" to "Goa",
        "31" to "Lakshadweep",
        "32" to "Kerala",
        "33" to "Tamil Nadu",
        "34" to "Puducherry",
        "35" to "Andaman and Nicobar Islands",
        "36" to "Telangana",
        "37" to "Andhra Pradesh",
        "38" to "Ladakh",
        "97" to "Other Territory",
        "99" to "Centre Jurisdiction",
    )

    val codes: Set<String> get() = names.keys

    fun isValid(code: String): Boolean = code in names

    fun englishName(code: String): String? = names[code]
}
