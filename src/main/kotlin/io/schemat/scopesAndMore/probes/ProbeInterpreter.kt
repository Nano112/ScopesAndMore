package io.schemat.scopesAndMore.probes
import kotlin.reflect.full.findAnnotation


// First, create an annotation to mark interpreters
@Target(AnnotationTarget.CLASS)
annotation class RegisterInterpreter(
    val id: String,
    val signalType: SignalType = SignalType.HEX
)

// Create a registry object
object InterpreterRegistry {
    private val interpreters = mutableMapOf<String, Pair<ValueInterpreter, SignalType>>()

    init {
        // Find all objects that implement ValueInterpreter and have RegisterInterpreter annotation
        ValueInterpreter::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .forEach { interpreter ->
                interpreter::class.findAnnotation<RegisterInterpreter>()?.let { annotation ->
                    interpreters[annotation.id] = interpreter to annotation.signalType
                }
            }
    }

    fun getInterpreter(id: String): Pair<ValueInterpreter, SignalType>? = interpreters[id]

    fun getAllInterpreters(): Map<String, Pair<ValueInterpreter, SignalType>> = interpreters.toMap()
}

enum class ReadOrder {
    MSB_FIRST, // Most significant bit first (leftmost)
    LSB_FIRST  // Least significant bit first (rightmost)
}

enum class SignalType {
    BINARY,  // 0 = off, >0 = on
    HEX      // Full 0-15 value
}

enum class Endianness {
    BIG, LITTLE
}

sealed interface ValueInterpreter {
    val name: String
    fun interpret(bits: List<Boolean>): String
    fun minimumBits(): Int
}

class ProbeInterpreter(
    var signalType: SignalType = SignalType.BINARY,
    var interpreter: ValueInterpreter = BinaryInterpreter,
    var endianness: Endianness = Endianness.LITTLE
) {
    fun interpret(values: List<Int>): String {
        // Handle endianness of input values first
        val processedValues = when (endianness) {
            Endianness.BIG -> values
            Endianness.LITTLE -> values.reversed()
        }

        // Then convert to bits based on signal type
        val bits = when (signalType) {
            SignalType.BINARY -> processedValues.map { it > 0 }
            SignalType.HEX -> processedValues.flatMap { value ->
                (0..3).map { bit -> (value and (1 shl bit)) != 0 }.reversed()
            }
        }

        if (bits.size < interpreter.minimumBits()) {
            return "Need ${interpreter.minimumBits()} bits, have ${bits.size}"
        }

        // For multi-bit interpreters, we need to chunk the bits
        val processedBits = when (interpreter.minimumBits()) {
            1 -> bits // No chunking needed for binary
            else -> {
                val chunkSize = interpreter.minimumBits()
                bits.chunked(chunkSize).flatten()
            }
        }

        return interpreter.interpret(processedBits)
    }
}

@RegisterInterpreter("binary", SignalType.BINARY)
object BinaryInterpreter : ValueInterpreter {
    override val name = "Binary"
    override fun interpret(bits: List<Boolean>) = bits.joinToString("") { if (it) "1" else "0" }
    override fun minimumBits() = 1
}

@RegisterInterpreter("hex")
object HexInterpreter : ValueInterpreter {
    override val name = "Hexadecimal"
    override fun interpret(bits: List<Boolean>): String {
        return bits.chunked(4).joinToString(" ") { chunk ->
            chunk.fold(0) { acc, bit -> (acc shl 1) or if (bit) 1 else 0 }
                .toString(16).uppercase()
        }
    }
    override fun minimumBits() = 4
}

@RegisterInterpreter("ascii")
object ASCIIInterpreter : ValueInterpreter {
    override val name = "ASCII"
    override fun interpret(bits: List<Boolean>): String {
        return bits.chunked(8).joinToString("") { chunk ->
            if (chunk.size == 8) {
                val byte = chunk.fold(0) { acc, bit -> (acc shl 1) or if (bit) 1 else 0 }
                byte.toChar().toString()
            } else ""
        }
    }
    override fun minimumBits() = 8
}

@RegisterInterpreter("float")
object FloatInterpreter : ValueInterpreter {
    override val name = "Float"
    override fun interpret(bits: List<Boolean>): String {
        if (bits.size != 32) return "Need 32 bits"
        val value = bits.fold(0) { acc, bit -> (acc shl 1) or if (bit) 1 else 0 }
        return Float.fromBits(value).toString()
    }
    override fun minimumBits() = 32
}

@RegisterInterpreter("uint")
object UnsignedIntInterpreter : ValueInterpreter {
    override val name = "Unsigned Int"
    override fun interpret(bits: List<Boolean>): String {
        // Directly convert to unsigned by treating first bit as part of value
        val value = bits.fold(0) { acc, bit ->
            (acc shl 1) or if (bit) 1 else 0
        }
        return value.toString()
    }
    override fun minimumBits() = 1
}

@RegisterInterpreter("int")
object SignedIntInterpreter : ValueInterpreter {
    override val name = "Signed Int"
    override fun interpret(bits: List<Boolean>): String {
        if (bits.isEmpty()) return "0"

        // For signed integers, first bit is sign bit
        val isNegative = bits.first()

        if (bits.size == 1) {
            return if (isNegative) "-1" else "0"
        }

        // Convert remaining bits to magnitude
        val magnitude = bits.drop(1).fold(0) { acc, bit ->
            (acc shl 1) or if (bit) 1 else 0
        }

        // If negative, apply two's complement
        return if (isNegative) {
            (-magnitude - (1 shl (bits.size - 1))).toString()
        } else {
            magnitude.toString()
        }
    }
    override fun minimumBits() = 1
}
