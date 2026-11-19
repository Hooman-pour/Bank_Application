package com.example.data.remote.faraboom

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FaraboomApiService {

    @FormUrlEncoded
    @POST("v1/oauth/token")
    suspend fun requestToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String? = null,
        @Field("redirect_uri") redirectUri: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): FaraboomTokenResponseDto

    @GET("v1/deposits")
    suspend fun getDeposits(
        @Header("Track-Id") trackId: String
    ): FaraboomDepositsResponseDto

    @GET("v1/deposits/{depositNumber}/statements")
    suspend fun getStatements(
        @Path("depositNumber") depositNumber: String,
        @Header("Track-Id") trackId: String,
        @Query("offset") offset: Int = 0,
        @Query("length") length: Int = 25
    ): FaraboomStatementsResponseDto

    @GET("v1/iban/inquiry")
    suspend fun inquireIban(
        @Query("iban") iban: String,
        @Header("Track-Id") trackId: String
    ): FaraboomIbanInquiryResponseDto

    @POST("v1/ach/transfer")
    suspend fun executeAchTransfer(
        @Header("Track-Id") trackId: String,
        @Body body: FaraboomAchTransferRequestDto
    ): FaraboomTransferResponseDto

    @POST("v1/rtgs/transfer")
    suspend fun executeRtgsTransfer(
        @Header("Track-Id") trackId: String,
        @Body body: FaraboomAchTransferRequestDto
    ): FaraboomTransferResponseDto

    @POST("v1/cards/transfer")
    suspend fun executeCardTransfer(
        @Header("Track-Id") trackId: String,
        @Body body: FaraboomCardTransferRequestDto
    ): FaraboomTransferResponseDto
}
