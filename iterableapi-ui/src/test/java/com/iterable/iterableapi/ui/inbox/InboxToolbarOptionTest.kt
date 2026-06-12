package com.iterable.iterableapi.ui.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class InboxToolbarOptionTest {

    @Test
    fun customCarriesLayoutRes() {
        val option = InboxToolbarOption.Custom(layoutRes = 42)
        assertEquals(42, option.layoutRes)
    }

    @Test
    fun customDataClassEqualityIsByValue() {
        assertEquals(InboxToolbarOption.Custom(7), InboxToolbarOption.Custom(7))
        assertNotEquals(InboxToolbarOption.Custom(7), InboxToolbarOption.Custom(8))
    }

    @Test
    fun dataObjectsAreReferenceSingletonsInProcess() {
        // Belt-and-suspenders: in-process the data object should be the same instance.
        assertSame(InboxToolbarOption.None, InboxToolbarOption.None)
        assertSame(InboxToolbarOption.Default, InboxToolbarOption.Default)
        assertSame(InboxToolbarOption.WithBackButton, InboxToolbarOption.WithBackButton)
    }

    @Test
    fun dataObjectsRoundTripPreserveSingletonIdentity() {
        // The `readResolve()` overrides on each data object must return the singleton
        // instance, not the freshly-deserialized copy — otherwise Java consumers
        // comparing with `==` after a Bundle/Intent round-trip get a surprise null
        // result. This test fails (assertSame downgrades to a different reference)
        // if the readResolve()s are ever removed.
        assertSame(InboxToolbarOption.None, roundTrip(InboxToolbarOption.None))
        assertSame(InboxToolbarOption.Default, roundTrip(InboxToolbarOption.Default))
        assertSame(InboxToolbarOption.WithBackButton, roundTrip(InboxToolbarOption.WithBackButton))
    }

    @Test
    fun customRoundTripsThroughJavaSerialization() {
        val original = InboxToolbarOption.Custom(layoutRes = 1234)
        val restored = roundTrip(original)
        assertEquals(original, restored)
        assertEquals(1234, (restored as InboxToolbarOption.Custom).layoutRes)
    }

    @Test
    fun whenBranchExhaustivelyDispatchesEachVariant() {
        val variants: List<InboxToolbarOption> = listOf(
            InboxToolbarOption.None,
            InboxToolbarOption.Default,
            InboxToolbarOption.WithBackButton,
            InboxToolbarOption.Custom(layoutRes = 99)
        )
        val labels = variants.map { option ->
            when (option) {
                InboxToolbarOption.None -> "none"
                InboxToolbarOption.Default -> "default"
                InboxToolbarOption.WithBackButton -> "back"
                is InboxToolbarOption.Custom -> "custom-${option.layoutRes}"
            }
        }
        assertEquals(listOf("none", "default", "back", "custom-99"), labels)
    }

    private fun roundTrip(option: InboxToolbarOption): InboxToolbarOption {
        val bytes = ByteArrayOutputStream().use { byteStream ->
            ObjectOutputStream(byteStream).use { it.writeObject(option) }
            byteStream.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            val restored = input.readObject()
            assertTrue("Round-tripped value must remain an InboxToolbarOption",
                restored is InboxToolbarOption)
            restored as InboxToolbarOption
        }
    }
}
