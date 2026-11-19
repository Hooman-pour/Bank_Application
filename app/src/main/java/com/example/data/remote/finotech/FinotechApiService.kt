package com.example.data.remote.finotech

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FinotechApiService {

    @POST("dev/v2/oauth2/token")
    suspend fun requestToken(
        @Header("Authorization") basicAuth: String,
        @Body body: FinotechTokenRequestDto
    ): FinotechEnvelope<FinotechTokenResultDto>

    @GET("oak/v2/clients/{clientId}/deposits")
    suspend fun getDeposits(
        @Path("clientId") clientId: String,
        @Query("trackId") trackId: String
    ): FinotechEnvelope<FinotechDepositsResultDto>

    @GET("oak/v2/clients/{clientId}/deposits/{deposit}/balance")
    suspend fun getDepositBalance(
        @Path("clientId") clientId: String,
        @Path("deposit") depositNumber: String,
        @Query("trackId") trackId: String
    ): FinotechEnvelope<FinotechDepositBalanceResultDto>

    @GET("oak/v2/clients/{clientId}/deposits/{deposit}/cc/statement")
    suspend fun getDepositStatement(
        @Path("clientId") clientId: String,
        @Path("deposit") depositNumber: String,
        @Query("trackId") trackId: String,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null,
        @Query("fromTime") fromTime: String? = null,
        @Query("toTime") toTime: String? = null,
        @Query("lastRecordNumber") lastRecordNumber: String? = null
    ): FinotechEnvelope<FinotechStatementsResultDto>

    @GET("facility/v2/clients/{clientId}/ibanInquiry")
    suspend fun inquireIban(
        @Path("clientId") clientId: String,
        @Query("iban") iban: String,
        @Query("trackId") trackId: String
    ): FinotechEnvelope<FinotechIbanInquiryResultDto>

    @POST("oak/v2/clients/{clientId}/transferTo")
    suspend fun transferTo(
        @Path("clientId") clientId: String,
        @Query("trackId") trackId: String,
        @Body body: FinotechTransferRequestDto
    ): FinotechEnvelope<FinotechTransferResultDto>
}
