package com.routecj.admin

import com.routecj.admin.core.util.EmailUtils
import com.routecj.admin.domain.model.AdminRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCreationSystemTest {

    @Test
    fun testDriverEmailGeneration_SimpleName() {
        val email = EmailUtils.generateDriverEmail("Ramesh Kumar")
        assertEquals("ramesh.kumar@routecj.com", email)
    }

    @Test
    fun testDriverEmailGeneration_MultipleSpacesAndSpecialChars() {
        val email = EmailUtils.generateDriverEmail("  Rajesh   M.   Sharma  ")
        assertEquals("rajesh.m.sharma@routecj.com", email)
    }

    @Test
    fun testDriverEmailGeneration_SingleName() {
        val email = EmailUtils.generateDriverEmail("Suresh")
        assertEquals("suresh@routecj.com", email)
    }

    @Test
    fun testAdminEmailGeneration_PresetRoles() {
        assertEquals("admin@routecj.com", EmailUtils.generateAdminEmail(AdminRole.ADMIN))
        assertEquals("godown.manager@routecj.com", EmailUtils.generateAdminEmail(AdminRole.GODOWN_MANAGER))
        assertEquals("dispatch.manager@routecj.com", EmailUtils.generateAdminEmail(AdminRole.DISPATCH_MANAGER))
        assertEquals("super.admin@routecj.com", EmailUtils.generateAdminEmail(AdminRole.SUPER_ADMIN))
    }

    @Test
    fun testAdminEmailGeneration_CustomName() {
        val email = EmailUtils.generateAdminEmail(AdminRole.DISPATCH_MANAGER, "Vikas Gupta")
        assertEquals("dispatch.vikas.gupta@routecj.com", email)
    }

    @Test
    fun testIsRouteCjEmail() {
        assertTrue(EmailUtils.isRouteCjEmail("ramesh.kumar@routecj.com"))
        assertTrue(EmailUtils.isRouteCjEmail("admin@routecj.com"))
        assertFalse(EmailUtils.isRouteCjEmail("ramesh@gmail.com"))
        assertFalse(EmailUtils.isRouteCjEmail("ramesh@otherroutecj.com"))
    }

    @Test
    fun testAdminRoleResolution() {
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromId("ADMIN001"))
        assertEquals(AdminRole.ADMIN, AdminRole.fromId("ADMIN002"))
        assertEquals(AdminRole.GODOWN_MANAGER, AdminRole.fromId("ADMIN003"))
        assertEquals(AdminRole.DISPATCH_MANAGER, AdminRole.fromId("ADMIN004"))
        assertEquals(AdminRole.DISPATCH_MANAGER, AdminRole.fromId("dispatch_manager"))
        assertEquals(AdminRole.GODOWN_MANAGER, AdminRole.fromId("Godown Manager"))
    }
}
