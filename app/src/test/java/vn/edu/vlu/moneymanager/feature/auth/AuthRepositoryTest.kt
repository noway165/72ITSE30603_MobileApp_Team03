package vn.edu.vlu.moneymanager.feature.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.moneymanager.data.repository.AuthRepository
import vn.edu.vlu.moneymanager.data.source.SampleData

/** AuthRepository is plain Kotlin; runTest skips its simulated network delay. */
class AuthRepositoryTest {

    private val repo = AuthRepository()

    @Test
    fun startsSignedOut() {
        assertNull(repo.currentUser.value)
    }

    @Test
    fun signIn_demoAccount_succeedsAndSetsUser() = runTest {
        val success = repo.signIn(SampleData.DEMO_EMAIL, SampleData.DEMO_PASSWORD)

        assertTrue(success)
        assertEquals(SampleData.DEMO_EMAIL, repo.currentUser.value?.email)
        assertFalse(repo.currentUser.value!!.isFirebaseAccount)
    }

    @Test
    fun signIn_wrongPassword_failsAndStaysSignedOut() = runTest {
        val success = repo.signIn(SampleData.DEMO_EMAIL, "wrong-password1")

        assertFalse(success)
        assertNull(repo.currentUser.value)
    }

    @Test
    fun signIn_unknownEmail_fails() = runTest {
        assertFalse(repo.signIn("nobody@vlu.edu.vn", SampleData.DEMO_PASSWORD))
    }

    @Test
    fun signIn_emailIgnoresCase_andIsStoredLowercase() = runTest {
        val success = repo.signIn(SampleData.DEMO_EMAIL.uppercase(), SampleData.DEMO_PASSWORD)

        assertTrue(success)
        assertEquals(SampleData.DEMO_EMAIL, repo.currentUser.value?.email)
    }

    @Test
    fun register_newEmail_signsInAndAllowsLaterSignIn() = runTest {
        val registered = repo.register("Lam@VLU.edu.vn", "secret123")

        assertTrue(registered)
        assertEquals("lam@vlu.edu.vn", repo.currentUser.value?.email)

        repo.signOut()
        assertTrue(repo.signIn("lam@vlu.edu.vn", "secret123"))
    }

    @Test
    fun register_existingEmail_failsWithAnyCase() = runTest {
        val registered = repo.register(SampleData.DEMO_EMAIL.uppercase(), "another123")

        assertFalse(registered)
        assertNull(repo.currentUser.value)
        // The old password still works: the failed register did not overwrite it
        assertTrue(repo.signIn(SampleData.DEMO_EMAIL, SampleData.DEMO_PASSWORD))
    }

    @Test
    fun signOut_clearsCurrentUser() = runTest {
        repo.signIn(SampleData.DEMO_EMAIL, SampleData.DEMO_PASSWORD)

        repo.signOut()

        assertNull(repo.currentUser.value)
    }
}
