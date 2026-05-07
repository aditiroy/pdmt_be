package com.lowes.permits.configuration;

import java.util.List;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import com.lowes.permits.entity.ConfigMongoEntity;
import com.lowes.permits.entity.LaborItemMongoEntity;
import com.lowes.permits.entity.OrderModMongoEntity;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.entity.SearchDictionaryMongoEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer {
	private static final List<Class<?>> ENTITY_CLASSES = List.of(
			ConfigMongoEntity.class,
			LaborItemMongoEntity.class,
			OrderModMongoEntity.class,
			PermitMongoEntity.class,
			SearchDictionaryMongoEntity.class);
	private final ReactiveMongoTemplate mongoTemplate;

	@EventListener(ContextRefreshedEvent.class)
	public void initIndicesAfterStartup() {
		log.info("Initializing MongoDB indexes at application startup...");
		try {
			MongoMappingContext mappingContext =
					(MongoMappingContext) mongoTemplate.getConverter().getMappingContext();
			IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

			for (Class<?> entityClass : ENTITY_CLASSES) {
				var indexOps = mongoTemplate.indexOps(entityClass);
				Flux.fromIterable(resolver.resolveIndexFor(entityClass))
						.flatMap(indexOps::createIndex)
						.doOnNext(indexName -> log.info("Successfully created index: {}", indexName))
						.doOnError(error -> log.error("Failed to create index: {}", error.getMessage()))
						.blockLast();
				log.info("MongoDB index initialization completed for {}", entityClass.getSimpleName());
			}
		} catch (Exception e) {
			log.error("Error during MongoDB index initialization: {}", e.getMessage(), e);
		}
	}
}
