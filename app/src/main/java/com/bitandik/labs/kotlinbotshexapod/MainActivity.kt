package com.bitandik.labs.kotlinbotshexapod

import android.app.Activity
import android.os.Bundle
import com.zugaldia.robocar.hardware.adafruit2348.AdafruitPwm
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class MainActivity : Activity() {
    private var norbertoTheSpider:Hexapod = Hexapod()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //norbertoTheSpider.calibrate()

        launch {
            norbertoTheSpider.sleep()
            delay(4000L)
            move()
        }

    }

    private suspend fun move(){
        when(norbertoTheSpider.state) {
            //HexapodState.CALIBRATE -> norbertoTheSpider.sleep()
            HexapodState.STAND -> norbertoTheSpider.sleep()
            HexapodState.SLEEP -> norbertoTheSpider.stand()
        }
        delay(4000L)
        move()
    }

    override fun onDestroy() {
        super.onDestroy()
        norbertoTheSpider.close()
    }
}

val sleepAngles: HashMap<JointPosition, Int> = hashMapOf(JointPosition.COXA to 180,
                                                            JointPosition.FEMUR to 0,
                                                            JointPosition.TIBIA to 0)


val standAngles: HashMap<JointPosition, Int> = hashMapOf(JointPosition.COXA to 100,
                                                            JointPosition.FEMUR to 50,
                                                            JointPosition.TIBIA to 30)

val calibrateAngles: HashMap<JointPosition, Int> = hashMapOf(JointPosition.COXA to 180,
                                                            JointPosition.FEMUR to 90,
                                                            JointPosition.TIBIA to 90)

enum class HexapodState {
    STAND, CALIBRATE, SLEEP
}


class Hexapod() {
    var state: HexapodState = HexapodState.STAND
    private var rightServoHat: ServoHat = ServoHat(0x41)
    private var leftServoHat: ServoHat = ServoHat()

    var legs: HashMap<LegPosition, Leg>

    init {
        legs = hashMapOf(LegPosition.FrontLeft to Leg(leftServoHat, 0, 1, 2),
                LegPosition.MiddleLeft to Leg(leftServoHat, 4, 5, 6),
                LegPosition.BackLeft to Leg(leftServoHat, 8, 9, 10),
                LegPosition.FrontRight to Leg(rightServoHat, 0, 1, 2),
                LegPosition.MiddleRight to Leg(rightServoHat, 4, 5, 6),
                LegPosition.BackRight to Leg(rightServoHat, 8, 9, 10))

    }

    fun stand(){
        state = HexapodState.STAND
        legs.forEach{
            position, leg ->
                if (isRight(position)) {
                    leg.moveTibia(standAngles[JointPosition.TIBIA]!!)
                    leg.moveFemur(standAngles[JointPosition.FEMUR]!!)
                } else {
                    leg.moveTibia(MAX_ANGLE - standAngles[JointPosition.TIBIA]!!)
                    leg.moveFemur(MAX_ANGLE - standAngles[JointPosition.FEMUR]!!)
                }
                when(position) {
                    LegPosition.FrontRight -> leg.moveCoxa(standAngles[JointPosition.COXA]!!)
                    LegPosition.MiddleRight -> leg.moveCoxa(MAX_ANGLE /2)
                    LegPosition.BackRight -> leg.moveCoxa(MAX_ANGLE - standAngles[JointPosition.COXA]!!)

                    LegPosition.FrontLeft -> leg.moveCoxa(MAX_ANGLE - standAngles[JointPosition.COXA]!!)
                    LegPosition.MiddleLeft -> leg.moveCoxa(MAX_ANGLE /2)
                    LegPosition.BackLeft -> leg.moveCoxa(standAngles[JointPosition.COXA]!!)
                }
        }
    }

    fun calibrate() {
        state = HexapodState.CALIBRATE
        legs.forEach { position, leg ->
            leg.moveTibia(calibrateAngles[JointPosition.TIBIA]!!)
            leg.moveFemur(calibrateAngles[JointPosition.FEMUR]!!)

            when(position) {
                LegPosition.FrontRight -> leg.moveCoxa(calibrateAngles[JointPosition.COXA]!!)
                LegPosition.MiddleRight -> leg.moveCoxa(MAX_ANGLE /2)
                LegPosition.BackRight -> leg.moveCoxa(MAX_ANGLE - calibrateAngles[JointPosition.COXA]!!)

                LegPosition.FrontLeft -> leg.moveCoxa(MAX_ANGLE - calibrateAngles[JointPosition.COXA]!!)
                LegPosition.MiddleLeft -> leg.moveCoxa(MAX_ANGLE /2)
                LegPosition.BackLeft -> leg.moveCoxa(calibrateAngles[JointPosition.COXA]!!)
            }
        }
    }

    fun sleep() {
        state = HexapodState.SLEEP
        legs.forEach { position, leg ->
            if (isRight(position)) {
                leg.moveTibia(sleepAngles[JointPosition.TIBIA]!!)
                leg.moveFemur(sleepAngles[JointPosition.FEMUR]!!)
            } else {
                leg.moveTibia(MAX_ANGLE - sleepAngles[JointPosition.TIBIA]!!)
                leg.moveFemur(MAX_ANGLE - sleepAngles[JointPosition.FEMUR]!!)
            }

            when(position) {
                LegPosition.FrontRight -> leg.moveCoxa(sleepAngles[JointPosition.COXA]!!)
                LegPosition.MiddleRight -> leg.moveCoxa(MAX_ANGLE /2)
                LegPosition.BackRight -> leg.moveCoxa(MAX_ANGLE - sleepAngles[JointPosition.COXA]!!)

                LegPosition.FrontLeft -> leg.moveCoxa(MAX_ANGLE - sleepAngles[JointPosition.COXA]!!)
                LegPosition.MiddleLeft -> leg.moveCoxa(MAX_ANGLE /2)
                LegPosition.BackLeft -> leg.moveCoxa(sleepAngles[JointPosition.COXA]!!)
            }
        }
    }

    private fun isRight(position: LegPosition): Boolean {
        return when(position) {
            LegPosition.FrontRight, LegPosition.MiddleRight, LegPosition.BackRight -> true
            else -> false
        }
    }

    fun close() {
        rightServoHat.close()
    }

}

enum class JointPosition {
    COXA, FEMUR, TIBIA
}

enum class LegPosition {
    FrontLeft, MiddleLeft, BackLeft,
    FrontRight, MiddleRight, BackRight
}

class Leg(private val servoHat: ServoHat, private val coxaPin: Int,
          private val femurPin: Int, private val tibiaPin:Int) {

    fun moveCoxa(angle: Int) {
        servoHat.setAngle(coxaPin, angle)
    }

    fun moveFemur(angle: Int) {
        servoHat.setAngle(femurPin, angle)
    }

    fun moveTibia(angle: Int) {
        servoHat.setAngle(tibiaPin, angle)
    }
}

const val I2C_DEVICE_NAME = "I2C1"
const val SERVO_HAT_I2C_ADDRESS = 0x40

const val MIN_PULSE_MS = 1.0
const val MAX_PULSE_MS = 2.0
const val MIN_ANGLE = 0
const val MAX_ANGLE = 180
const val MIN_CHANNEL = 0
const val MAX_CHANNEL = 15

class ServoHat(val i2cAddr:Int = SERVO_HAT_I2C_ADDRESS) {
    private val adafruitPWM: AdafruitPwm = AdafruitPwm(I2C_DEVICE_NAME, i2cAddr)
    /*
     https://github.com/adafruit/Adafruit_Python_PCA9685/blob/master/examples/simpletest.py
     1,000,000 us per second
     40 Hz
     12 bits of resolution
    */
    private val pulseLength = (1000000 / 60) / 4096
    private val servos: HashMap<Int, Int> = HashMap()

    init {
        adafruitPWM.setPwmFreq(60)
        for (channel in MIN_CHANNEL..15) {
            servos[channel] = 0
        }
    }

    fun setAngle(channel: Int, angle: Int) {
        if (angle in MIN_ANGLE..MAX_ANGLE && channel in MIN_CHANNEL..MAX_CHANNEL) {
            servos[channel] = angle
            val normalizedAngle:Double = (angle - MIN_ANGLE) / (MAX_ANGLE - MIN_ANGLE).toDouble()
            val pulse = MIN_PULSE_MS + (MAX_PULSE_MS - MIN_PULSE_MS) * normalizedAngle

            val dutyCycle = ((pulse * 1000) / pulseLength).toInt()
            adafruitPWM.setPwm(channel, 0, dutyCycle)
        }
    }

    fun getAngle(channel: Int): Int {
        var angle = 0
        if (channel in MIN_CHANNEL..MAX_CHANNEL) {
            servos[channel]?.let {
                angle = it
            }
        }
        return angle
    }

    fun close() {
        adafruitPWM.close()
    }
}