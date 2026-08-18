package org.quantumbadger.redreader.common

class Optional<E> private constructor(private val mValue: E?) {
    class OptionalHasNoValueException : RuntimeException()

    val isPresent: Boolean
        get() = mValue != null

    val isEmpty: Boolean
        get() = mValue == null

    fun get(): E {
        if (mValue == null) {
            throw OptionalHasNoValueException()
        }

        return mValue
    }

    fun asNullable(): E? {
        return mValue
    }

    fun orElse(alternative: E): E {
        if (mValue == null) {
            return alternative
        } else {
            return mValue
        }
    }

    fun orElse(alternative: Optional<E>): Optional<E> {
        if (mValue == null) {
            return alternative
        } else {
            return of<E>(mValue)
        }
    }

    fun orElseNull(): E? {
        return mValue
    }

    @Throws(T::class)
    fun <T : Exception?> orThrow(
        factory: GenericFactory<T, RuntimeException>
    ): E {
        if (mValue == null) {
            throw factory.create()
        }

        return mValue
    }

    fun <R> map(function: FunctionOneArgWithReturn<E, R>): Optional<R> {
        if (mValue == null) {
            return empty<R>()
        } else {
            return of<R>(function.apply(mValue))
        }
    }

    fun <R> flatMap(
        function: FunctionOneArgWithReturn<E, Optional<R>>
    ): Optional<R> {
        if (mValue == null) {
            return empty<R>()
        } else {
            return function.apply(mValue)
        }
    }

    fun apply(function: FunctionOneArgNoReturn<E>) {
        if (mValue != null) {
            function.apply(mValue)
        }
    }

    fun <R> filter(
        function: FunctionOneArgWithReturn<E, Optional<R>>
    ): Optional<R> {
        if (mValue == null) {
            return empty<R>()
        } else {
            return function.apply(mValue)
        }
    }

    fun ifPresent(consumer: Consumer<E>) {
        if (mValue != null) {
            consumer.consume(mValue)
        }
    }

    override fun hashCode(): Int {
        if (mValue == null) {
            return 0x28734823 // Random value
        } else {
            return mValue.hashCode()
        }
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is Optional<*>) {
            return false
        }

        if (mValue == null) {
            return obj.mValue == null
        }

        return mValue == obj.mValue
    }

    override fun toString(): String {
        if (mValue == null) {
            return "<empty>"
        } else {
            return mValue.toString()
        }
    }

    companion object {
        private val EMPTY: Optional<*> = Optional<Any?>(null)

        @JvmStatic
        fun <E> empty(): Optional<E> {
            @Suppress("UNCHECKED_CAST")
            return EMPTY as Optional<E>
        }

        fun <E> of(value: E): Optional<E> {
            return Optional<E>(value)
        }

        fun <E> ofNullable(value: E?): Optional<E> {
            if (value == null) {
                return empty()
            }

            return Optional<E>(value)
        }
    }
}
