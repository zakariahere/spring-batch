package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dan Garrette
 * @since 2.0.2
 */
public class FaultTolerantExceptionClassesTests {

	//
	// TODO BATCH-1318: Commented out tests are related to this issue
	//

	private static ApplicationContext ctx;

	private static JobRepository jobRepository;

	private static JobLauncher jobLauncher;

	private static SkipReaderStub<String> reader;

	private static SkipWriterStub<String> writer;

	@SuppressWarnings("unchecked")
	@BeforeClass
	public static void setCtx() {
		ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/core/step/item/FaultTolerantExceptionClassesTests-context.xml");
		jobRepository = (JobRepository) ctx.getBean("jobRepository");
		jobLauncher = (JobLauncher) ctx.getBean("jobLauncher");
		reader = (SkipReaderStub<String>) ctx.getBean("reader");
		writer = (SkipWriterStub<String>) ctx.getBean("writer");
	}

	@Before
	public void setup() {
		reader.clear();
		writer.clear();
	}

	@Test
	public void testNonSkippable() throws Exception {
		writer.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("nonSkippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testNonSkippableChecked() throws Exception {
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("nonSkippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testDefaultFatal() throws Exception {
		writer.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// TODO BATCH-1318: assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testSkippable() throws Exception {
		writer.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	public void testFatal() throws Exception {
		writer.setExceptionType(FatalRuntimeException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testDefaultFatalChecked() throws Exception {
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// TODO BATCH-1318: assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testSkippableChecked() throws Exception {
		writer.setExceptionType(SkippableException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		// TODO BATCH-1318: assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// TODO BATCH-1318: assertEquals("[1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	public void testFatalChecked() throws Exception {
		writer.setExceptionType(FatalException.class);
		StepExecution stepExecution = launchStep("skippableStep");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// TODO BATCH-1318: assertEquals("[1, 2, 3]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testRetryableDefaultFatal() throws Exception {
		writer.setExceptionType(RuntimeException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testRetryableSkippable() throws Exception {
		writer.setExceptionType(SkippableRuntimeException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	public void testRetryableFatal() throws Exception {
		writer.setExceptionType(FatalRuntimeException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// TODO BATCH-1318: assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testRetryableDefaultFatalChecked() throws Exception {
		writer.setExceptionType(Exception.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[]", writer.getCommitted().toString());
	}

	@Test
	public void testRetryableSkippableChecked() throws Exception {
		writer.setExceptionType(SkippableException.class);
		StepExecution stepExecution = launchStep("retryable");
		// TODO BATCH-1318: assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		// TODO BATCH-1318: assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3, 4]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[1, 2, 4]", writer.getCommitted().toString());
	}

	@Test
	public void testRetryableFatalChecked() throws Exception {
		writer.setExceptionType(FatalException.class);
		StepExecution stepExecution = launchStep("retryable");
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("[1, 2, 3, 1, 2, 3, 1, 2, 3]", writer.getWritten().toString());
		// TODO BATCH-1318: assertEquals("[]", writer.getCommitted().toString());
	}

	private StepExecution launchStep(String stepName) throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException {
		SimpleJob job = new SimpleJob();
		job.setName("job");
		job.setJobRepository(jobRepository);

		List<Step> stepsToExecute = new ArrayList<Step>();
		stepsToExecute.add((Step) ctx.getBean(stepName));
		job.setSteps(stepsToExecute);

		JobExecution jobExecution = jobLauncher.run(job, new JobParametersBuilder().addLong("timestamp",
				new Date().getTime()).toJobParameters());
		return jobExecution.getStepExecutions().iterator().next();
	}

}
