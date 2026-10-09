package app.sentinel.authenticator

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Run on a dedicated emulator configured with PIN 246810. Uses only a public RFC test seed. */
@RunWith(AndroidJUnit4::class)
class EnrollmentUiTest {
    @Test fun enrollLockAndReloadAccount() {
        val inst=InstrumentationRegistry.getInstrumentation();val context=inst.targetContext
        val device=UiDevice.getInstance(inst)
        fun launch() { context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));device.waitForIdle() }
        fun click(text:String) {
            val item=device.wait(Until.findObject(By.text(text)),15000) ?: error("Missing UI control: $text")
            item.click();device.waitForIdle();android.os.SystemClock.sleep(1000)
        }
        fun unlock() {
            click("Unlock authenticator")
            device.waitForIdle();android.os.SystemClock.sleep(1200)
            "246810".forEach { device.pressKeyCode(android.view.KeyEvent.KEYCODE_0+(it-'0')) }
            device.pressEnter()
            assertTrue(device.wait(Until.hasObject(By.text("Sentinel")),20000))
        }
        launch()
        if(device.wait(Until.hasObject(By.text("Get started")),3000))click("Get started")
        else device.findObject(By.scrollable(true))?.scroll(Direction.DOWN,1f)
        if(device.hasObject(By.text("Get started")))click("Get started")
        unlock()
        val add=device.wait(Until.findObjects(By.text("Add account")),10000)
        assertTrue(add.isNotEmpty());add.last().click()
        assertTrue(device.wait(Until.hasObject(By.text("Review account")),10000))
        val fields=device.findObjects(By.clazz("android.widget.EditText"));assertEquals(3,fields.size)
        fields[0].text="RFC Test";fields[1].text="test@example.test";fields[2].text="GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        click("Review account");assertTrue(device.wait(Until.hasObject(By.text("Check your account")),10000));click("Add account")
        assertTrue(device.wait(Until.hasObject(By.text("test@example.test")),15000))
        device.pressHome();android.os.SystemClock.sleep(1500);launch()
        assertTrue(device.wait(Until.hasObject(By.text("Your vault is locked")),15000))
        assertFalse(device.hasObject(By.text("test@example.test")))
        unlock()
        assertTrue(device.wait(Until.hasObject(By.text("test@example.test")),15000))
    }
}
