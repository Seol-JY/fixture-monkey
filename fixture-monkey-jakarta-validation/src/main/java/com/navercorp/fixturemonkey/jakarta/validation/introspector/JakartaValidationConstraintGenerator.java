/*
 * Fixture Monkey
 *
 * Copyright (c) 2021-present NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.fixturemonkey.jakarta.validation.introspector;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.navercorp.fixturemonkey.api.constraint.JavaConstraintGenerator;
import com.navercorp.fixturemonkey.api.constraint.JavaContainerConstraint;
import com.navercorp.fixturemonkey.api.constraint.JavaDateTimeConstraint;
import com.navercorp.fixturemonkey.api.constraint.JavaDecimalConstraint;
import com.navercorp.fixturemonkey.api.constraint.JavaIntegerConstraint;
import com.navercorp.fixturemonkey.api.constraint.JavaStringConstraint;
import com.navercorp.fixturemonkey.api.constraint.JavaStringConstraint.PatternConstraint;
import com.navercorp.fixturemonkey.api.generator.ArbitraryGeneratorContext;

@API(since = "0.4.10", status = Status.MAINTAINED)
public final class JakartaValidationConstraintGenerator implements JavaConstraintGenerator {
	@Override
	@Nullable
	public JavaStringConstraint generateStringConstraint(ArbitraryGeneratorContext context) {
		BigInteger min = null;
		BigInteger max = null;
		boolean digits = false;
		boolean notNull = context.findAnnotation(NotNull.class).isPresent();
		boolean notBlank = context.findAnnotation(NotBlank.class).isPresent();
		boolean email = context.findAnnotation(Email.class).isPresent();

		if (notBlank || context.findAnnotation(NotEmpty.class).isPresent()) {
			min = BigInteger.ONE;
		}

		Optional<Size> size = context.findAnnotation(Size.class);
		if (size.isPresent()) {
			int minValue = size.map(Size::min).get();
			if (min == null) {
				min = BigInteger.valueOf(minValue);
			} else if (minValue > 1) {
				min = BigInteger.valueOf(minValue);
			}

			max = BigInteger.valueOf(size.map(Size::max).get());
		}

		Optional<Digits> digitsAnnotation = context.findAnnotation(Digits.class);
		if (digitsAnnotation.isPresent()) {
			digits = true;
			notBlank = true;

			BigInteger maxValue = digitsAnnotation.map(Digits::integer).map(BigInteger::valueOf).get();
			if (max == null) {
				max = maxValue;
			} else if (max.compareTo(maxValue) > 0) {
				max = maxValue;
			}
		}

		Optional<Pattern> patternAnnotation = context.findAnnotation(Pattern.class);
		PatternConstraint patternConstraint = null;
		if (patternAnnotation.isPresent()) {
			String regexp = patternAnnotation.get().regexp();
			int[] flags = Arrays.stream(patternAnnotation.get().flags()).mapToInt(Flag::getValue).toArray();
			patternConstraint = new PatternConstraint(regexp, flags);
		}

		if (min == null && max == null && !digits && !notNull && !notBlank && patternConstraint == null && !email) {
			return null;
		}

		return new JavaStringConstraint(min, max, digits, notNull, notBlank, patternConstraint, email);
	}

	@Override
	@Nullable
	public JavaIntegerConstraint generateIntegerConstraint(ArbitraryGeneratorContext context) {
		BigInteger positiveMin = null;
		BigInteger positiveMax = null;
		BigInteger negativeMax = null;
		BigInteger negativeMin = null;

		Optional<Digits> digits = context.findAnnotation(Digits.class);
		if (digits.isPresent()) {
			BigInteger value = BigInteger.ONE;
			int integer = digits.get().integer();
			if (integer > 1) {
				value = BigInteger.TEN.pow(integer - 1);
			}
			positiveMax = value.multiply(BigInteger.TEN).subtract(BigInteger.ONE);
			positiveMin = value;
			negativeMax = positiveMin.negate();
			negativeMin = positiveMax.negate();
		}

		Optional<Min> minAnnotation = context.findAnnotation(Min.class);
		if (minAnnotation.isPresent()) {
			BigInteger minValue = minAnnotation.map(Min::value).map(BigInteger::valueOf).get();
			if (minValue.compareTo(BigInteger.ZERO) >= 0) {
				if (positiveMin == null) {
					positiveMin = minValue;
				} else {
					positiveMin = positiveMin.min(minValue);
				}
			} else {
				if (negativeMin == null) {
					negativeMin = minValue;
				} else {
					negativeMin = negativeMin.min(minValue);
				}
			}
		}

		Optional<DecimalMin> decimalMinAnnotation = context.findAnnotation(DecimalMin.class);
		if (decimalMinAnnotation.isPresent()) {
			BigInteger decimalMin = new BigInteger(
				decimalMinAnnotation
					.get()
					.value()
			);

			if (!decimalMinAnnotation.map(DecimalMin::inclusive).get()) {
				decimalMin = decimalMin.add(BigInteger.ONE);
			}

			if (decimalMin.compareTo(BigInteger.ZERO) >= 0) {
				if (positiveMin == null) {
					positiveMin = decimalMin;
				} else {
					positiveMin = positiveMin.min(decimalMin);
				}
			} else {
				if (negativeMin == null) {
					negativeMin = decimalMin;
				} else {
					negativeMin = negativeMin.min(decimalMin);
				}
			}
		}

		Optional<Max> maxAnnotation = context.findAnnotation(Max.class);
		if (maxAnnotation.isPresent()) {
			BigInteger maxValue = maxAnnotation.map(Max::value).map(BigInteger::valueOf).get();
			if (maxValue.compareTo(BigInteger.ZERO) > 0) {
				if (positiveMax == null) {
					positiveMax = maxValue;
				} else {
					positiveMax = positiveMax.max(maxValue);
				}
			} else {
				if (negativeMax == null) {
					negativeMax = maxValue;
				} else {
					negativeMax = negativeMax.max(maxValue);
				}
			}
		}

		Optional<DecimalMax> decimalMaxAnnotation = context.findAnnotation(DecimalMax.class);
		if (decimalMaxAnnotation.isPresent()) {
			BigInteger decimalMax = new BigInteger(
				decimalMaxAnnotation
					.get()
					.value()
			);

			if (!decimalMaxAnnotation.map(DecimalMax::inclusive).get()) {
				decimalMax = decimalMax.subtract(BigInteger.ONE);
			}

			if (decimalMax.compareTo(BigInteger.ZERO) > 0) {
				if (positiveMax == null) {
					positiveMax = decimalMax;
				} else {
					positiveMax = positiveMax.max(decimalMax);
				}
			} else {
				if (negativeMax == null) {
					negativeMax = decimalMax;
				} else {
					negativeMax = negativeMax.max(decimalMax);
				}
			}
		}

		if (context.findAnnotation(Negative.class).isPresent()) {
			if (negativeMax == null || negativeMax.compareTo(BigInteger.ZERO) > 0) {
				negativeMax = BigInteger.valueOf(-1);
			}
		}

		if (context.findAnnotation(NegativeOrZero.class).isPresent()) {
			if (negativeMax == null || negativeMax.compareTo(BigInteger.ZERO) > 0) {
				negativeMax = BigInteger.ZERO;
			}
		}

		if (context.findAnnotation(Positive.class).isPresent()) {
			if (positiveMin == null || positiveMin.compareTo(BigInteger.ZERO) < 0) {
				positiveMin = BigInteger.ONE;
			}
		}

		if (context.findAnnotation(PositiveOrZero.class).isPresent()) {
			if (positiveMin == null || positiveMin.compareTo(BigInteger.ZERO) < 0) {
				positiveMin = BigInteger.ZERO;
			}
		}

		Type type = context.getResolvedType();
		if (negativeMin != null) {
			if ((type == Long.class || type == long.class) && negativeMin.compareTo(BIG_INTEGER_MIN_LONG) < 0) {
				negativeMin = BIG_INTEGER_MIN_LONG;
			}
			if ((type == Integer.class || type == int.class) && negativeMin.compareTo(BIG_INTEGER_MIN_INT) < 0) {
				negativeMin = BIG_INTEGER_MIN_INT;
			}
			if ((type == Short.class || type == short.class) && negativeMin.compareTo(BIG_INTEGER_MIN_SHORT) < 0) {
				negativeMin = BIG_INTEGER_MIN_SHORT;
			}
			if ((type == Byte.class || type == byte.class) && negativeMin.compareTo(BIG_INTEGER_MIN_BYTE) < 0) {
				negativeMin = BIG_INTEGER_MIN_BYTE;
			}
		}
		if (positiveMax != null) {
			if ((type == Long.class || type == long.class) && positiveMax.compareTo(BIG_INTEGER_MAX_LONG) > 0) {
				positiveMax = BIG_INTEGER_MAX_LONG;
			}
			if ((type == Integer.class || type == int.class) && positiveMax.compareTo(BIG_INTEGER_MAX_INT) > 0) {
				positiveMax = BIG_INTEGER_MAX_INT;
			}
			if ((type == Short.class || type == short.class) && positiveMax.compareTo(BIG_INTEGER_MAX_SHORT) > 0) {
				positiveMax = BIG_INTEGER_MAX_SHORT;
			}
			if ((type == Byte.class || type == byte.class) && positiveMax.compareTo(BIG_INTEGER_MAX_BYTE) > 0) {
				positiveMax = BIG_INTEGER_MAX_BYTE;
			}
		}

		if (positiveMin == null && positiveMax == null && negativeMin == null && negativeMax == null) {
			return null;
		}

		return new JavaIntegerConstraint(positiveMin, positiveMax, negativeMin, negativeMax);
	}

	@Override
	@Nullable
	public JavaDecimalConstraint generateDecimalConstraint(ArbitraryGeneratorContext context) {
		BigDecimal min = null;
		Boolean minInclusive = null;
		BigDecimal max = null;
		Boolean maxInclusive = null;
		Integer scale = null;

		Optional<Min> minAnnotation = context.findAnnotation(Min.class);
		if (minAnnotation.isPresent()) {
			min = BigDecimal.valueOf(minAnnotation.get().value());
			minInclusive = true;
		}

		Optional<DecimalMin> decimalMinAnnotation = context.findAnnotation(DecimalMin.class);
		if (decimalMinAnnotation.isPresent()) {
			BigDecimal newMin = new BigDecimal(decimalMinAnnotation.get().value());

			if (min == null || newMin.compareTo(min) > 0
				|| (newMin.compareTo(min) == 0 && !decimalMinAnnotation.get().inclusive())) {

				min = newMin;
				minInclusive = decimalMinAnnotation.get().inclusive();
			}
		}

		Optional<Max> maxAnnotation = context.findAnnotation(Max.class);
		if (maxAnnotation.isPresent()) {
			max = BigDecimal.valueOf(maxAnnotation.get().value());
			maxInclusive = true;
		}

		Optional<DecimalMax> decimalMaxAnnotation = context.findAnnotation(DecimalMax.class);
		if (decimalMaxAnnotation.isPresent()) {
			BigDecimal newMax = new BigDecimal(decimalMaxAnnotation.get().value());

			if (max == null || newMax.compareTo(max) < 0
				|| (newMax.compareTo(max) == 0 && !decimalMaxAnnotation.get().inclusive())) {

				max = newMax;
				maxInclusive = decimalMaxAnnotation.get().inclusive();
			}
		}

		if (context.findAnnotation(Positive.class).isPresent()) {
			if (min == null || BigDecimal.ZERO.compareTo(min) > 0
				|| (BigDecimal.ZERO.compareTo(min) == 0 && minInclusive)) {
				min = BigDecimal.ZERO;
				minInclusive = false;
			}
		}

		if (context.findAnnotation(PositiveOrZero.class).isPresent()) {
			if (min == null || BigDecimal.ZERO.compareTo(min) > 0) {
				min = BigDecimal.ZERO;
				minInclusive = true;
			}
		}

		if (context.findAnnotation(Negative.class).isPresent()) {
			if (max == null || BigDecimal.ZERO.compareTo(max) < 0
				|| (BigDecimal.ZERO.compareTo(max) == 0 && maxInclusive)) {
				max = BigDecimal.ZERO;
				maxInclusive = false;
			}
		}

		if (context.findAnnotation(NegativeOrZero.class).isPresent()) {
			if (max == null || BigDecimal.ZERO.compareTo(max) < 0) {
				max = BigDecimal.ZERO;
				maxInclusive = true;
			}
		}

		Optional<Digits> digitsAnn = context.findAnnotation(Digits.class);
		if (digitsAnn.isPresent()) {
			Digits digits = digitsAnn.get();
			int integerDigits = digits.integer();
			int fractionDigits = digits.fraction();

			StringBuilder maxBuilder = new StringBuilder();
			for (int i = 0; i < integerDigits; i++) {
				maxBuilder.append('9');
			}
			if (fractionDigits > 0) {
				maxBuilder.append('.');
				for (int i = 0; i < fractionDigits; i++) {
					maxBuilder.append('9');
				}
			}
			BigDecimal digitsMax = new BigDecimal(maxBuilder.toString());
			BigDecimal digitsMin = digitsMax.negate();

			if (max == null || digitsMax.compareTo(max) < 0) {
				max = digitsMax;
				maxInclusive = true;
			}
			if (min == null || digitsMin.compareTo(min) > 0) {
				min = digitsMin;
				minInclusive = true;
			}

			scale = digits.fraction();
		}

		if (min == null && max == null) {
			return null;
		}

		boolean isPositiveMin = min != null && min.compareTo(BigDecimal.ZERO) >= 0;
		boolean isPositiveMax = max != null && max.compareTo(BigDecimal.ZERO) >= 0;
		boolean isNegativeMin = min != null && min.compareTo(BigDecimal.ZERO) < 0;
		boolean isNegativeMax = max != null && max.compareTo(BigDecimal.ZERO) < 0;

		if (isPositiveMax && max.equals(BigDecimal.ZERO)) {
			return new JavaDecimalConstraint(
				null,
				null,
				null,
				null,
				isNegativeMin ? min : null,
				isNegativeMin ? minInclusive : null,
				BigDecimal.ZERO,
				isNegativeMax ? maxInclusive : null,
				scale
			);
		}

		if (isNegativeMin && isPositiveMax) {
			return new JavaDecimalConstraint(
				BigDecimal.ZERO,
				true,
				max,
				maxInclusive,
				min,
				minInclusive,
				BigDecimal.ZERO,
				false,
				scale
			);
		}

		return new JavaDecimalConstraint(
			isPositiveMin ? min : null,
			isPositiveMin ? minInclusive : null,
			isPositiveMax ? max : null,
			isPositiveMax ? maxInclusive : null,
			isNegativeMin ? min : null,
			isNegativeMin ? minInclusive : null,
			isNegativeMax ? max : null,
			isNegativeMax ? maxInclusive : null,
			scale
		);

	}

	@Override
	@Nullable
	public JavaContainerConstraint generateContainerConstraint(ArbitraryGeneratorContext context) {
		Integer minSize = null;
		Integer maxSize = null;
		boolean notEmpty = context.findAnnotation(NotEmpty.class).isPresent();

		Optional<Size> sizeAnnotation = context.findAnnotation(Size.class);
		if (sizeAnnotation.isPresent()) {
			Size size = sizeAnnotation.get();
			minSize = size.min();
			maxSize = size.max();
		}
		if (minSize == null && maxSize == null && !notEmpty) {
			return null;
		}

		return new JavaContainerConstraint(
			minSize,
			maxSize,
			notEmpty
		);
	}

	@Override
	@Nullable
	public JavaDateTimeConstraint generateDateTimeConstraint(ArbitraryGeneratorContext context) {
		Supplier<LocalDateTime> min = null;
		if (context.findAnnotation(Future.class).isPresent()) {
			min = () -> LocalDateTime.now().plusSeconds(3);    // 3000 is buffer for future time
		} else if (context.findAnnotation(FutureOrPresent.class).isPresent()) {
			min = () -> LocalDateTime.now().plusSeconds(2);    // 2000 is buffer for future time
		}

		Supplier<LocalDateTime> max = null;
		if (context.findAnnotation(Past.class).isPresent()) {
			max = () -> LocalDateTime.now().minusSeconds(1);
		} else if (context.findAnnotation(PastOrPresent.class).isPresent()) {
			max = LocalDateTime::now;
		}

		if (min == null && max == null) {
			return null;
		}

		return new JavaDateTimeConstraint(min, max);
	}
}
