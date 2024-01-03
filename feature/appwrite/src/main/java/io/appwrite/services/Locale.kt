package io.appwrite.services

import io.appwrite.Client
import io.appwrite.models.*

/**
 * The Locale service allows you to customize your app based on your users&#039; location.
 **/
class Locale(client: Client) : Service(client) {

    /**
     * Get user locale
     *
     * Get the current user location based on IP. Returns an object with user country code, country name, continent name, continent code, ip address and suggested currency. You can use the locale header to get the data in a supported language.([IP Geolocation by DB-IP](https://db-ip.com))
     *
     * @return [io.appwrite.models.Locale]
     */
    suspend fun get(
    ): io.appwrite.models.Locale {
        val apiPath = "/locale"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.Locale = {
            io.appwrite.models.Locale.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.Locale::class.java,
            converter,
        )
    }


    /**
     * List Locale Codes
     *
     * List of all locale codes in [ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes).
     *
     * @return [io.appwrite.models.LocaleCodeList]
     */
    suspend fun listCodes(
    ): io.appwrite.models.LocaleCodeList {
        val apiPath = "/locale/codes"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.LocaleCodeList = {
            io.appwrite.models.LocaleCodeList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.LocaleCodeList::class.java,
            converter,
        )
    }


    /**
     * List continents
     *
     * List of all continents. You can use the locale header to get the data in a supported language.
     *
     * @return [io.appwrite.models.ContinentList]
     */
    suspend fun listContinents(
    ): io.appwrite.models.ContinentList {
        val apiPath = "/locale/continents"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.ContinentList = {
            io.appwrite.models.ContinentList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.ContinentList::class.java,
            converter,
        )
    }


    /**
     * List countries
     *
     * List of all countries. You can use the locale header to get the data in a supported language.
     *
     * @return [io.appwrite.models.CountryList]
     */
    suspend fun listCountries(
    ): io.appwrite.models.CountryList {
        val apiPath = "/locale/countries"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.CountryList = {
            io.appwrite.models.CountryList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.CountryList::class.java,
            converter,
        )
    }


    /**
     * List EU countries
     *
     * List of all countries that are currently members of the EU. You can use the locale header to get the data in a supported language.
     *
     * @return [io.appwrite.models.CountryList]
     */
    suspend fun listCountriesEU(
    ): io.appwrite.models.CountryList {
        val apiPath = "/locale/countries/eu"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.CountryList = {
            io.appwrite.models.CountryList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.CountryList::class.java,
            converter,
        )
    }


    /**
     * List countries phone codes
     *
     * List of all countries phone codes. You can use the locale header to get the data in a supported language.
     *
     * @return [io.appwrite.models.PhoneList]
     */
    suspend fun listCountriesPhones(
    ): io.appwrite.models.PhoneList {
        val apiPath = "/locale/countries/phones"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.PhoneList = {
            io.appwrite.models.PhoneList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.PhoneList::class.java,
            converter,
        )
    }


    /**
     * List currencies
     *
     * List of all currencies, including currency symbol, name, plural, and decimal digits for all major and minor currencies. You can use the locale header to get the data in a supported language.
     *
     * @return [io.appwrite.models.CurrencyList]
     */
    suspend fun listCurrencies(
    ): io.appwrite.models.CurrencyList {
        val apiPath = "/locale/currencies"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.CurrencyList = {
            io.appwrite.models.CurrencyList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.CurrencyList::class.java,
            converter,
        )
    }


    /**
     * List languages
     *
     * List of all languages classified by ISO 639-1 including 2-letter code, name in English, and name in the respective language.
     *
     * @return [io.appwrite.models.LanguageList]
     */
    suspend fun listLanguages(
    ): io.appwrite.models.LanguageList {
        val apiPath = "/locale/languages"

        val apiParams = mutableMapOf<String, Any?>(
        )
        val apiHeaders = mutableMapOf(
            "content-type" to "application/json",
        )
        val converter: (Any) -> io.appwrite.models.LanguageList = {
            io.appwrite.models.LanguageList.from(map = it as Map<String, Any>)
        }
        return client.call(
            "GET",
            apiPath,
            apiHeaders,
            apiParams,
            responseType = io.appwrite.models.LanguageList::class.java,
            converter,
        )
    }


}
