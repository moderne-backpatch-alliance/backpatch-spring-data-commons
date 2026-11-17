/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverterUnitTests.SampleInterface;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link ProxyingHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 * @soundtrack Karlijn Langendijk & Sönke Meinen - Englishman In New York (Sting,
 *             https://www.youtube.com/watch?v=O7LZsqrnaaA)
 */
public class ProxyingHandlerMethodArgumentResolverUnitTests {

	ProxyingHandlerMethodArgumentResolver resolver = new ProxyingHandlerMethodArgumentResolver(
			() -> new DefaultConversionService(), true);

	@Test // DATACMNS-776
	void supportAnnotatedInterface() throws Exception {

		Method method = Controller.class.getMethod("with", AnnotatedInterface.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-776
	void supportsUnannotatedInterfaceFromUserPackage() throws Exception {

		Method method = Controller.class.getMethod("with", SampleInterface.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-776
	void doesNotSupportUnannotatedInterfaceFromSpringNamespace() throws Exception {

		Method method = Controller.class.getMethod("with", UnannotatedInterface.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // DATACMNS-776
	void doesNotSupportCoreJavaType() throws Exception {

		Method method = Controller.class.getMethod("with", List.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-3495, test written by us
	void capsCollectionGrowthFromRequestParameters() throws Exception {
		assertThat(bindItems(resolver)).isNullOrEmpty();
	}

	@Test // GH-3495, test written by us
	void collectionLimitCanBeOverridden() throws Exception {

		SpringProperties.setProperty("spring.data.web.projection.collection-limit", "3000");

		try {
			assertThat(bindItems(new ProxyingHandlerMethodArgumentResolver(() -> new DefaultConversionService(), true)))
					.hasSize(2001);
		} finally {
			SpringProperties.setProperty("spring.data.web.projection.collection-limit", null);
		}
	}

	private static List<String> bindItems(ProxyingHandlerMethodArgumentResolver resolver) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("items[2000]", "value");

		MethodParameter parameter = new MethodParameter(Controller.class.getMethod("with", ItemsPayload.class), 0);

		return ((ItemsPayload) resolver.createAttribute("payload", parameter, null, new ServletWebRequest(request)))
				.getItems();
	}

	@ProjectedPayload
	interface ItemsPayload {
		List<String> getItems();
	}

	@ProjectedPayload
	interface AnnotatedInterface {}

	interface UnannotatedInterface {}

	interface Controller {

		void with(AnnotatedInterface param);

		void with(ItemsPayload param);

		void with(UnannotatedInterface param);

		void with(SampleInterface param);

		void with(List<Object> param);
	}
}
