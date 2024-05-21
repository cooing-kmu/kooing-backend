package com.alpha.kooing.user.service

import com.alpha.kooing.config.jwt.JwtTokenProvider
import com.alpha.kooing.external.AmazonS3Service
import com.alpha.kooing.user.dto.*
import com.alpha.kooing.user.entity.UserConcernKeyword
import com.alpha.kooing.user.entity.UserInterestKeyword
import com.alpha.kooing.user.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.jvm.optionals.getOrNull

@Service
class UserService(
    private val jwtTokenProvider: JwtTokenProvider,
    private val amazonS3Service: AmazonS3Service,
    private val userRepository: UserRepository,
    private val userInterestKeywordRepository: UserInterestKeywordRepository,
    private val userConcernKeywordRepository: UserConcernKeywordRepository,
    private val interestKeywordRepository: InterestKeywordRepository,
    private val concernKeywordRepository: ConcernKeywordRepository,
) {

    @Transactional(readOnly = true)
    fun getUser(token: String): UserDetail {
        val userEmail = jwtTokenProvider.getJwtEmail(token)
        val user = userRepository.findByEmail(userEmail).getOrNull() ?: throw Exception("로그인 유저 정보가 올바르지 않습니다.")
        return UserDetail(
            user.username,
            user.roleType,
            user.profileMessage,
            user.profileImageUrl,
            user.userInterestKeyword.map {
                it.interestKeyword.name
            },
            user.userConcernKeyword.map {
                it.concernKeyword.name
            }
        )
    }

    @Transactional
    fun updateUserProfile(token: String, request: UpdateUserProfileRequest, profileImage: MultipartFile?) {
        val userEmail = jwtTokenProvider.getJwtEmail(token)
        val user = userRepository.findByEmail(userEmail).getOrNull() ?: throw Exception("로그인 유저 정보가 올바르지 않습니다.")
        user.updateProfile(request.name, request.profileMessage)
        if (profileImage != null) {
            val profileImageUrl = amazonS3Service.upload(profileImage, "profile")
            user.updateProfileImage(profileImageUrl)
        }
        userRepository.save(user)
    }

    @Transactional
    fun updateUserMatchingStatus(token: String, request: UpdateUserMatchingStatusRequest) {
        val userEmail = jwtTokenProvider.getJwtEmail(token)
        val user = userRepository.findByEmail(userEmail).getOrNull() ?: throw Exception("로그인 유저 정보가 올바르지 않습니다.")
        user.updateMatchingStatus(request.isMatchingActive)
        userRepository.save(user)
    }

    @Transactional
    fun updateUserMatchingKeyword(token: String, request: UpdateUserMatchingKeywordRequest) {
        val userEmail = jwtTokenProvider.getJwtEmail(token)
        val user = userRepository.findByEmail(userEmail).getOrNull() ?: throw Exception("로그인 유저 정보가 올바르지 않습니다.")
        val interestKeywords = interestKeywordRepository.findAll()
        val concernKeywords = concernKeywordRepository.findAll()
        userInterestKeywordRepository.deleteAllById(user.userInterestKeyword.map { it.id })
        userConcernKeywordRepository.deleteAllById(user.userConcernKeyword.map { it.id })
        userInterestKeywordRepository.saveAll(
            interestKeywords
                .filter { it.name in request.interestKeyword }
                .map { updateInterestKeyword ->
                    UserInterestKeyword(null, user, updateInterestKeyword)
                }
        )
        userConcernKeywordRepository.saveAll(
            concernKeywords
                .filter { it.name in request.concernKeyword }
                .map { updateConcernKeyword ->
                    UserConcernKeyword(null, user, updateConcernKeyword)
                }
        )
    }

    /**
     * 1. income 소득 30-40
     * 2. housing 주거 44-77
     * 3. finance 금융 80-94
     * 4. education 진학 98-107
     * 5. employment 취업 110-132
     * 6. health 의료 및 건강 136-159
     * 7. miscellaneous 기타 162-170
     * 8. tips 알아두면 꿀 팁 174-185
     */
    @Transactional(readOnly = true)
    fun getUserCheckList(): GetUserCheckListResponse {
        val incomeList = getImageUrlList(30, 40)
        val housingList = getImageUrlList(44, 77)
        val financeList = getImageUrlList(80, 94)
        val educationList = getImageUrlList(98, 107)
        val employmentList = getImageUrlList(110, 132)
        val healthList = getImageUrlList(136, 159)
        val miscellaneousList = getImageUrlList(162, 170)
        val tipsList = getImageUrlList(174, 185)
        return GetUserCheckListResponse(
            incomeList,
            housingList,
            financeList,
            educationList,
            employmentList,
            healthList,
            miscellaneousList,
            tipsList
        )
    }

    private fun getImageUrlList(startPage: Int, endPage: Int): List<String> {
        return (startPage..endPage).map { page ->
            "https://cooing-bucket.s3.ap-northeast-2.amazonaws.com/checklist/2024%E1%84%82%E1%85%A7%E1%86%AB+%E1%84%8C%E1%85%A1%E1%84%85%E1%85%B5%E1%86%B8%E1%84%8C%E1%85%A5%E1%86%BC%E1%84%87%E1%85%A9%E1%84%87%E1%85%AE%E1%86%A8+%E1%84%8E%E1%85%AC%E1%84%8C%E1%85%A9%E1%86%BC_page-${String.format("%04d", page)}.jpg"
        }
    }


}