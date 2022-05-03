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

package com.navercorp.fixturemonkey.api.generator;

import javax.annotation.Nullable;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import com.navercorp.fixturemonkey.api.random.Randoms;

@API(since = "0.4.0", status = Status.EXPERIMENTAL)
public final class ArbitraryContainerInfo {
	private final int elementMinSize;
	private final int elementMaxSize;

	public ArbitraryContainerInfo(
		int elementMinSize,
		int elementMaxSize
	) {
		this.elementMinSize = elementMinSize;
		this.elementMaxSize = elementMaxSize;
	}

	public int getElementMinSize() {
		return this.elementMinSize;
	}

	public int getElementMaxSize() {
		return this.elementMaxSize;
	}

	public int getRandomSize() {
		if (this.elementMinSize == this.elementMaxSize) {
			return this.elementMinSize;
		}

		return this.elementMinSize + Randoms.nextInt(this.elementMaxSize - this.elementMinSize);
	}

	public ArbitraryContainerInfo withElementMinSize(@Nullable Integer minSize) {
		if (minSize == null) {
			return this;
		}
		return new ArbitraryContainerInfo(minSize, this.elementMaxSize);
	}

	public ArbitraryContainerInfo withElementMaxSize(@Nullable Integer maxSize) {
		if (maxSize == null) {
			return this;
		}
		return new ArbitraryContainerInfo(this.elementMinSize, maxSize);
	}

	@Override
	public String toString() {
		return "ArbitraryPropertyContainerInfo{"
			+ "elementMinSize=" + elementMinSize
			+ ", elementMaxSize=" + elementMaxSize
			+ '}';
	}
}
