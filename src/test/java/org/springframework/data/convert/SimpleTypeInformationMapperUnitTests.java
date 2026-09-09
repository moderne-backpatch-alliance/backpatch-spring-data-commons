/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.convert;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link SimpleTypeInformationMapper}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class SimpleTypeInformationMapperUnitTests {

	SimpleTypeInformationMapper mapper = new SimpleTypeInformationMapper();

	@Test // CVE-2026-41716
	void unresolvableAliasesDoNotGrowTheCacheWithoutBound() {

		// loadClass() returns Optional.empty() for an alias it cannot load rather than throwing,
		// so before the fix every distinct alias string -- including one an attacker supplies as a
		// stored type hint -- was retained in the cache permanently.
		for (int i = 0; i < 5000; i++) {
			assertThat(mapper.resolveTypeFrom(Alias.of("com.example.NotAClass" + i))).isNull();
		}

		Object cache = ReflectionTestUtils.getField(mapper, "cache");
		Integer size = ReflectionTestUtils.invokeMethod(cache, "size");

		assertThat(size).as("cache entries retained from 5000 unresolvable aliases").isLessThanOrEqualTo(1024);
	}

	@Test // CVE-2026-41716
	void resolvableAliasesAreStillResolvedAfterTheCacheIsFull() {

		for (int i = 0; i < 5000; i++) {
			mapper.resolveTypeFrom(Alias.of("com.example.NotAClass" + i));
		}

		assertThat(mapper.resolveTypeFrom(Alias.of("java.lang.String")))
				.isEqualTo(ClassTypeInformation.from(String.class));
	}

	@Test
	void resolvesTypeByLoadingClass() {

		TypeInformation<?> type = mapper.resolveTypeFrom(Alias.of("java.lang.String"));

		TypeInformation<?> expected = ClassTypeInformation.from(String.class);

		assertThat(type).isEqualTo(expected);
	}

	@Test // GH-2508
	void usesConfiguredClassloader() {

		mapper.setBeanClassLoader(HidingClassLoader.hide(SimpleTypeInformationMapperUnitTests.class));
		TypeInformation<?> type = mapper
				.resolveTypeFrom(Alias.of("org.springframework.data.convert.SimpleTypeInformationMapperUnitTests.User"));

		assertThat(type).isNull();
	}

	@Test
	void returnsNullForNonStringKey() {
		assertThat(mapper.resolveTypeFrom(Alias.of(new Object()))).isNull();
	}

	@Test
	void returnsNullForEmptyTypeKey() {
		assertThat(mapper.resolveTypeFrom(Alias.of(""))).isNull();
	}

	@Test
	void returnsNullForUnloadableClass() {

		assertThat(mapper.resolveTypeFrom(Alias.of("Foo"))).isNull();
	}

	@Test
	void usesFullyQualifiedClassNameAsTypeKey() {

		assertThat(mapper.createAliasFor(ClassTypeInformation.from(String.class)))
				.isEqualTo(Alias.of(String.class.getName()));
	}

	static class User {

	}
}
