package com.alpha.kooing.support.repository

import com.alpha.kooing.support.entity.SupportBusiness
import org.springframework.data.jpa.repository.JpaRepository

interface SupportIBusinessRepository: JpaRepository<SupportBusiness, Long>