/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.aggregate.counter.sink;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.analytics.metrics.AggregateCounter;
import org.springframework.analytics.metrics.AggregateCounterRepository;
import org.springframework.analytics.metrics.AggregateCounterResolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.app.test.redis.RedisTestSupport;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Tests the aggregate-counter module.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = "classes:TestAggregateCounterSinkApplication.class")
public abstract class AggregateCounterTests {

	@Rule
	public RedisTestSupport redisTestSupport = new RedisTestSupport();

	private static final String AGGREGATE_COUNTER_NAME = "foo";

	private static final String AGGREGATE_COUNTER_NAME_2 = "bar";

	@Autowired
	protected Sink sink;

	@Autowired
	protected AggregateCounterRepository aggregateCounterRepository;

	@Before
	@After
	public void clear() {
		aggregateCounterRepository.reset(AGGREGATE_COUNTER_NAME);
		aggregateCounterRepository.reset(AGGREGATE_COUNTER_NAME_2);
	}


	@TestPropertySource(properties = "aggregate-counter.name=" + AGGREGATE_COUNTER_NAME)
	public static class NullTimefieldAggregateCounterTests extends AggregateCounterTests {

		@Test
		public void testCountNow() {
			this.sink.input().send(new GenericMessage<Object>(""));
			AggregateCounter counts = this.aggregateCounterRepository.getCounts(AGGREGATE_COUNTER_NAME, 5,
					AggregateCounterResolution.hour);
			assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 0, 0, 1 }));
			aggregateCounterRepository.reset(AGGREGATE_COUNTER_NAME);
		}

	}

	@TestPropertySource(properties = {
			"aggregate-counter.name=" + AGGREGATE_COUNTER_NAME,
			"aggregate-counter.timeField=payload.ts",
			"aggregate-counter.dateFormat=dd/MM/yyyy" })
	public static class CountWithTimestampInMessageAndCustomFormatTests extends AggregateCounterTests {

		@Test
		public void testCountWithTimestampInMessageAndCustomFormat() {
			this.sink.input().send(new GenericMessage<Object>(Collections.singletonMap("ts", "14/10/1978")));
			DateTime endDate = new DateTime(1980, 1, 1, 0, 0);
			AggregateCounter counts = this.aggregateCounterRepository.getCounts(AGGREGATE_COUNTER_NAME, 5, endDate,
					AggregateCounterResolution.year);
			assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 1, 0, 0 }));
		}

	}

	@TestPropertySource(properties = {
			"aggregate-counter.name=" + AGGREGATE_COUNTER_NAME,
			"aggregate-counter.incrementExpression=payload" })
	public static class CountWithCustomIncrementTests extends AggregateCounterTests {

		@Test
		public void testCountWithCustomIncrement() {
			this.sink.input().send(new GenericMessage<Object>("43"));
			AggregateCounter counts = this.aggregateCounterRepository.getCounts(AGGREGATE_COUNTER_NAME, 5,
					AggregateCounterResolution.hour);
			assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 0, 0, 43 }));
		}

	}

	@TestPropertySource(properties = "aggregate-counter.nameExpression=payload.counterName" )
	public static class CountWithNameExpressionTests extends AggregateCounterTests {

		@Test
		public void testCountWithNameExpression() {
			this.sink.input().send(new GenericMessage<Object>(
					Collections.singletonMap("counterName", AGGREGATE_COUNTER_NAME)));
			AggregateCounter counts = this.aggregateCounterRepository.getCounts(AGGREGATE_COUNTER_NAME, 5,
					AggregateCounterResolution.hour);
			assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 0, 0, 1 }));
		}

	}

	@TestPropertySource(properties = "aggregate-counter.nameExpression=payload.counterName" )
	public static class CounterListTest extends AggregateCounterTests {

		@Test
		public void testCountWithNameExpression() {
			this.sink.input().send(new GenericMessage<Object>(
					Collections.singletonMap("counterName", AGGREGATE_COUNTER_NAME)));
			this.sink.input().send(new GenericMessage<Object>(
					Collections.singletonMap("counterName", AGGREGATE_COUNTER_NAME_2)));
			assertThat(this.aggregateCounterRepository.list(), hasItems(AGGREGATE_COUNTER_NAME, AGGREGATE_COUNTER_NAME_2));
		}

	}

}
