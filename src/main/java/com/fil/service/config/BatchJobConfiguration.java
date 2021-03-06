package com.fil.service.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.fil.service.model.Customer;
import com.fil.service.task.CustomerItemProcessor;
import com.fil.service.task.JobCompletionNotificationListener;

@Configuration
@EnableBatchProcessing
public class BatchJobConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(BatchJobConfiguration.class);
	
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;
	
	@Autowired
	JobCompletionNotificationListener listener;

	@Bean
	public FlatFileItemReader<Customer> reader() {
		FlatFileItemReader<Customer> reader = new FlatFileItemReader<Customer>();
		reader.setStrict(false);
		reader.setResource(new ClassPathResource("sample-data.csv"));
		reader.setLineMapper(new DefaultLineMapper<Customer>() {
			{
				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames(new String[] { "firstName", "lastName", "accountNo" });
					}
				});
				setFieldSetMapper(new BeanWrapperFieldSetMapper<Customer>() {
					{
						setTargetType(Customer.class);
					}
				});
			}
		});
		return reader;
	}

	@Bean
	public CustomerItemProcessor processor() {
		return new CustomerItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Customer> writer() {
		JdbcBatchItemWriter<Customer> writer = new JdbcBatchItemWriter<Customer>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Customer>());
		writer.setSql(
				"INSERT INTO CUSTOMER (first_name, last_name,account_no) VALUES (:firstName, :lastName, :accountNo)");
		writer.setDataSource(dataSource);
		return writer;
	}

	@Bean
	public Job importUserJob(JobCompletionNotificationListener listener) {
		return jobBuilderFactory.get("importUserJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(readerStep()).end().build();
	}

	@Bean
	public Step readerStep() {
		return stepBuilderFactory.get("readerStep").<Customer, Customer> chunk(10).reader(reader())
				.processor(processor()).writer(writer()).build();
	}
	
	// just one job as an app
	 /*@Bean
	    public Job job() {
	        return jobBuilderFactory.get("job").start(stepBuilderFactory.get("jobStep1").tasklet(new Tasklet() {
	            @Override
	            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
	                logger.info("Job was run");
	                return RepeatStatus.FINISHED;
	            }
	        }).build()).build();
	    }*/
}
