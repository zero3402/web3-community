import { Pool } from 'pg';
import { performance } from 'perf_hooks';

// Database performance testing
const pool = new Pool({
  user: process.env.DB_USER || 'perfuser',
  host: process.env.DB_HOST || 'localhost',
  database: process.env.DB_NAME || 'perftestdb',
  password: process.env.DB_PASSWORD || 'perfpass',
  port: process.env.DB_PORT || 5432,
  max: 20, // Maximum number of clients in the pool
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

class DatabasePerformanceTest {
  async runTests() {
    console.log('🚀 Starting Database Performance Tests...\n');

    try {
      await this.setupTestData();
      await this.testConnectionPooling();
      await this.testQueryPerformance();
      await this.testConcurrentQueries();
      await this.testIndexPerformance();
      await this.cleanup();
      
      console.log('✅ All database performance tests completed successfully!');
    } catch (error) {
      console.error('❌ Database performance test failed:', error);
      throw error;
    }
  }

  async setupTestData() {
    console.log('📊 Setting up test data...');
    
    const client = await pool.connect();
    try {
      // Create test table
      await client.query(`
        CREATE TABLE IF NOT EXISTS performance_test (
          id SERIAL PRIMARY KEY,
          user_id INTEGER,
          title VARCHAR(255),
          content TEXT,
          category_id INTEGER,
          created_at TIMESTAMP DEFAULT NOW(),
          updated_at TIMESTAMP DEFAULT NOW(),
          metadata JSONB
        );
      `);

      // Create indexes
      await client.query(`
        CREATE INDEX IF NOT EXISTS idx_performance_test_user_id ON performance_test(user_id);
        CREATE INDEX IF NOT EXISTS idx_performance_test_category_id ON performance_test(category_id);
        CREATE INDEX IF NOT EXISTS idx_performance_test_created_at ON performance_test(created_at);
        CREATE INDEX IF NOT EXISTS idx_performance_test_metadata ON performance_test USING GIN(metadata);
      `);

      // Insert test data
      const insertStart = performance.now();
      for (let i = 0; i < 1000; i++) {
        await client.query(`
          INSERT INTO performance_test (user_id, title, content, category_id, metadata)
          VALUES ($1, $2, $3, $4, $5)
        `, [
          Math.floor(Math.random() * 100) + 1,
          `Test Post ${i}`,
          `This is test content for post ${i} with some additional text to simulate real content`,
          Math.floor(Math.random() * 10) + 1,
          JSON.stringify({ tags: [`tag${i % 10}`], views: Math.floor(Math.random() * 1000) })
        ]);
      }
      const insertEnd = performance.now();
      
      console.log(`   ✅ Inserted 1000 test records in ${(insertEnd - insertStart).toFixed(2)}ms`);
    } finally {
      client.release();
    }
  }

  async testConnectionPooling() {
    console.log('🔗 Testing connection pooling...');
    
    const startTime = performance.now();
    const promises = [];
    
    // Create 50 concurrent connections
    for (let i = 0; i < 50; i++) {
      promises.push(
        pool.query('SELECT pg_sleep(0.1), $1 as iteration', [i])
          .then(result => result.rows[0])
      );
    }
    
    const results = await Promise.all(promises);
    const endTime = performance.now();
    
    console.log(`   ✅ 50 concurrent queries completed in ${(endTime - startTime).toFixed(2)}ms`);
    console.log(`   📊 Average time per query: ${((endTime - startTime) / 50).toFixed(2)}ms`);
    console.log(`   📊 Pool total count: ${pool.totalCount}`);
    console.log(`   📊 Pool idle count: ${pool.idleCount}`);
    console.log(`   📊 Pool waiting count: ${pool.waitingCount}`);
  }

  async testQueryPerformance() {
    console.log('⚡ Testing query performance...');
    
    const client = await pool.connect();
    try {
      // Test simple SELECT
      const simpleStart = performance.now();
      const simpleResult = await client.query('SELECT COUNT(*) FROM performance_test');
      const simpleEnd = performance.now();
      console.log(`   ✅ Simple COUNT query: ${(simpleEnd - simpleStart).toFixed(2)}ms`);

      // Test complex query with JOIN
      const complexStart = performance.now();
      const complexResult = await client.query(`
        SELECT 
          user_id,
          COUNT(*) as post_count,
          AVG(LENGTH(content)) as avg_content_length,
          MAX(created_at) as last_post_date
        FROM performance_test 
        WHERE created_at > NOW() - INTERVAL '1 hour'
        GROUP BY user_id
        ORDER BY post_count DESC
        LIMIT 10
      `);
      const complexEnd = performance.now();
      console.log(`   ✅ Complex aggregation query: ${(complexEnd - complexStart).toFixed(2)}ms`);

      // Test full-text search
      const searchStart = performance.now();
      const searchResult = await client.query(`
        SELECT title, content, ts_rank(search_vector, query) as rank
        FROM (
          SELECT 
            title, 
            content,
            to_tsvector('english', title || ' ' || content) as search_vector
          FROM performance_test
        ) t,
        plainto_tsquery('english', 'test content') query
        WHERE search_vector @@ query
        ORDER BY rank DESC
        LIMIT 10
      `);
      const searchEnd = performance.now();
      console.log(`   ✅ Full-text search query: ${(searchEnd - searchStart).toFixed(2)}ms`);

      // Test JSONB query
      const jsonbStart = performance.now();
      const jsonbResult = await client.query(`
        SELECT metadata->>'tags' as tags, metadata->>'views' as views
        FROM performance_test
        WHERE metadata @> '{"views": 500}'
        ORDER BY (metadata->>'views')::NUMERIC DESC
        LIMIT 20
      `);
      const jsonbEnd = performance.now();
      console.log(`   ✅ JSONB query: ${(jsonbEnd - jsonbStart).toFixed(2)}ms`);

    } finally {
      client.release();
    }
  }

  async testConcurrentQueries() {
    console.log('🔄 Testing concurrent query performance...');
    
    const concurrentQueries = 100;
    const startTime = performance.now();
    
    const promises = [];
    for (let i = 0; i < concurrentQueries; i++) {
      promises.push(
        pool.query(`
          SELECT * FROM performance_test 
          WHERE user_id = $1 
          ORDER BY created_at DESC 
          LIMIT 10
        `, [Math.floor(Math.random() * 100) + 1])
      );
    }
    
    const results = await Promise.all(promises);
    const endTime = performance.now();
    
    console.log(`   ✅ ${concurrentQueries} concurrent queries completed in ${(endTime - startTime).toFixed(2)}ms`);
    console.log(`   📊 Average query time: ${((endTime - startTime) / concurrentQueries).toFixed(2)}ms`);
    console.log(`   📊 Queries per second: ${(concurrentQueries / ((endTime - startTime) / 1000)).toFixed(2)}`);
  }

  async testIndexPerformance() {
    console.log('📈 Testing index performance...');
    
    const client = await pool.connect();
    try {
      // Test query without index hint
      const noIndexStart = performance.now();
      await client.query(`
        SELECT * FROM performance_test 
        WHERE title LIKE '%test%'
        AND created_at > NOW() - INTERVAL '1 day'
        ORDER BY created_at DESC
        LIMIT 50
      `);
      const noIndexEnd = performance.now();
      
      // Test query with index
      const withIndexStart = performance.now();
      await client.query(`
        SELECT * FROM performance_test 
        WHERE user_id = 1
        AND category_id = 2
        ORDER BY created_at DESC
        LIMIT 50
      `);
      const withIndexEnd = performance.now();
      
      console.log(`   ✅ Query without index: ${(noIndexEnd - noIndexStart).toFixed(2)}ms`);
      console.log(`   ✅ Query with index: ${(withIndexEnd - withIndexStart).toFixed(2)}ms`);
      
      // Analyze query plan
      const explainResult = await client.query(`
        EXPLAIN ANALYZE
        SELECT * FROM performance_test 
        WHERE user_id = 1 
        AND category_id = 2
        ORDER BY created_at DESC
        LIMIT 10
      `);
      
      console.log('   📊 Query execution plan:');
      explainResult.rows.forEach(row => console.log(`      ${row['QUERY PLAN']}`));
      
    } finally {
      client.release();
    }
  }

  async cleanup() {
    console.log('🧹 Cleaning up test data...');
    
    const client = await pool.connect();
    try {
      await client.query('DROP TABLE IF EXISTS performance_test');
      console.log('   ✅ Test table dropped');
    } finally {
      client.release();
    }
  }
}

// Run tests if this file is executed directly
if (require.main === module) {
  const tester = new DatabasePerformanceTest();
  tester.runTests()
    .then(() => {
      console.log('\n🎉 Database performance tests completed!');
      process.exit(0);
    })
    .catch((error) => {
      console.error('\n💥 Database performance tests failed:', error);
      process.exit(1);
    });
}

export default DatabasePerformanceTest;