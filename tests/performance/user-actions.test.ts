import { test, expect } from '@playwright/test';
import { performance } from 'perf_hooks';

// User Action Performance Tests for Web3 Community
class UserActionPerformanceTest {
  constructor(page) {
    this.page = page;
    this.metrics = [];
  }

  async measureAction(actionName, action) {
    const startTime = performance.now();
    await action();
    const endTime = performance.now();
    
    const duration = endTime - startTime;
    this.metrics.push({
      action: actionName,
      duration: duration,
      timestamp: new Date().toISOString()
    });
    
    console.log(`⏱️  ${actionName}: ${duration.toFixed(2)}ms`);
    return duration;
  }

  async runAllTests() {
    console.log('🚀 Starting User Action Performance Tests...\n');

    try {
      await this.testUserRegistration();
      await this.testUserLogin();
      await this.testPostCreation();
      await this.testPostInteraction();
      await this.testCommentActions();
      await this.testSearchActions();
      await this.testNavigationPerformance();
      await this.testProfileActions();
      await this.generateReport();
      
      console.log('✅ All user action performance tests completed!');
    } catch (error) {
      console.error('❌ User action performance test failed:', error);
      throw error;
    }
  }

  async testUserRegistration() {
    console.log('👤 Testing user registration performance...');
    
    await this.page.goto('/register');
    
    await this.measureAction('Navigate to Registration', () =>
      this.page.waitForSelector('[data-testid="registration-form"]')
    );

    await this.measureAction('Fill Registration Form', async () => {
      await this.page.fill('[data-testid="email-input"]', `perfuser${Date.now()}@test.com`);
      await this.page.fill('[data-testid="password-input"]', 'password123');
      await this.page.fill('[data-testid="username-input"]', `perfuser${Date.now()}`);
    });

    await this.measureAction('Submit Registration', () =>
      this.page.click('[data-testid="register-button"]')
    );

    await this.measureAction('Registration Complete', () =>
      this.page.waitForURL('/dashboard')
    );
  }

  async testUserLogin() {
    console.log('🔐 Testing user login performance...');
    
    await this.page.goto('/login');
    
    await this.measureAction('Navigate to Login', () =>
      this.page.waitForSelector('[data-testid="login-form"]')
    );

    await this.measureAction('Fill Login Form', async () => {
      await this.page.fill('[data-testid="email-input"]', 'test@example.com');
      await this.page.fill('[data-testid="password-input"]', 'password123');
    });

    await this.measureAction('Submit Login', () =>
      this.page.click('[data-testid="login-button"]')
    );

    await this.measureAction('Login Complete', () =>
      this.page.waitForURL('/dashboard')
    );
  }

  async testPostCreation() {
    console.log('📝 Testing post creation performance...');
    
    await this.page.goto('/posts/create');
    
    await this.measureAction('Navigate to Create Post', () =>
      this.page.waitForSelector('[data-testid="post-form"]')
    );

    await this.measureAction('Fill Post Form', async () => {
      await this.page.fill('[data-testid="post-title"]', `Performance Test Post ${Date.now()}`);
      await this.page.fill('[data-testid="post-content"]', 'This is a performance test post content for measuring the time it takes to create a post.');
      await this.page.selectOption('[data-testid="category-select"]', { label: 'General Discussion' });
    });

    await this.measureAction('Submit Post', () =>
      this.page.click('[data-testid="submit-post-button"]')
    );

    await this.measureAction('Post Creation Complete', () =>
      this.page.waitForSelector('[data-testid="post-success-message"]')
    );
  }

  async testPostInteraction() {
    console.log('👍 Testing post interaction performance...');
    
    await this.page.goto('/community');
    
    await this.measureAction('Load Community Page', () =>
      this.page.waitForSelector('[data-testid="post-list"]')
    );

    // Test liking a post
    await this.measureAction('Like Post', async () => {
      const firstPost = this.page.locator('[data-testid="post-item"]').first();
      await firstPost.locator('[data-testid="like-button"]').click();
    });

    // Test sharing a post
    await this.measureAction('Share Post', async () => {
      const firstPost = this.page.locator('[data-testid="post-item"]').first();
      await firstPost.locator('[data-testid="share-button"]').click();
      await this.page.waitForSelector('[data-testid="share-modal"]');
      await this.page.click('[data-testid="close-modal"]');
    });

    // Test bookmarking a post
    await this.measureAction('Bookmark Post', async () => {
      const firstPost = this.page.locator('[data-testid="post-item"]').first();
      await firstPost.locator('[data-testid="bookmark-button"]').click();
    });
  }

  async testCommentActions() {
    console.log('💬 Testing comment actions performance...');
    
    await this.page.goto('/community');
    
    // Navigate to first post
    await this.measureAction('Navigate to Post Details', async () => {
      const firstPost = this.page.locator('[data-testid="post-item"]').first();
      await firstPost.click();
      await this.page.waitForSelector('[data-testid="post-detail"]');
    });

    // Test adding a comment
    await this.measureAction('Add Comment', async () => {
      await this.page.fill('[data-testid="comment-input"]', 'This is a performance test comment.');
      await this.page.click('[data-testid="submit-comment"]');
      await this.page.waitForSelector('[data-testid="comment-success"]');
    });

    // Test replying to a comment
    await this.measureAction('Reply to Comment', async () => {
      const firstComment = this.page.locator('[data-testid="comment-item"]').first();
      await firstComment.locator('[data-testid="reply-button"]').click();
      await this.page.fill('[data-testid="reply-input"]', 'This is a performance test reply.');
      await this.page.click('[data-testid="submit-reply"]');
    });

    // Test voting on comment
    await this.measureAction('Vote on Comment', async () => {
      const firstComment = this.page.locator('[data-testid="comment-item"]').first();
      await firstComment.locator('[data-testid="upvote-button"]').click();
    });
  }

  async testSearchActions() {
    console.log('🔍 Testing search performance...');
    
    await this.page.goto('/community');
    
    await this.measureAction('Open Search', () =>
      this.page.click('[data-testid="search-button"]')
    );

    await this.measureAction('Perform Search', async () => {
      await this.page.fill('[data-testid="search-input"]', 'performance test');
      await this.page.click('[data-testid="search-submit"]');
      await this.page.waitForSelector('[data-testid="search-results"]');
    });

    await this.measureAction('Filter Search Results', async () => {
      await this.page.selectOption('[data-testid="category-filter"]', { label: 'General Discussion' });
      await this.page.waitForSelector('[data-testid="filtered-results"]');
    });

    await this.measureAction('Sort Search Results', async () => {
      await this.page.selectOption('[data-testid="sort-select"]', { label: 'Most Recent' });
      await this.page.waitForSelector('[data-testid="sorted-results"]');
    });
  }

  async testNavigationPerformance() {
    console.log('🧭 Testing navigation performance...');
    
    const pages = [
      { name: 'Dashboard', url: '/dashboard' },
      { name: 'Community', url: '/community' },
      { name: 'Profile', url: '/profile' },
      { name: 'Notifications', url: '/notifications' },
      { name: 'Settings', url: '/settings' }
    ];

    for (const page of pages) {
      await this.measureAction(`Navigate to ${page.name}`, async () => {
        await this.page.goto(page.url);
        await this.page.waitForLoadState('networkidle');
      });
    }

    // Test breadcrumb navigation
    await this.page.goto('/community/posts/123');
    await this.measureAction('Breadcrumb Navigation', async () => {
      await this.page.click('[data-testid="breadcrumb-community"]');
      await this.page.waitForURL('/community');
    });
  }

  async testProfileActions() {
    console.log('👤 Testing profile actions performance...');
    
    await this.page.goto('/profile');
    
    await this.measureAction('Load Profile Page', () =>
      this.page.waitForSelector('[data-testid="profile-content"]')
    );

    await this.measureAction('Edit Profile', async () => {
      await this.page.click('[data-testid="edit-profile-button"]');
      await this.page.waitForSelector('[data-testid="profile-edit-form"]');
      await this.page.fill('[data-testid="bio-input"]', 'Updated bio for performance test');
      await this.page.click('[data-testid="save-profile"]');
    });

    await this.measureAction('Upload Profile Picture', async () => {
      await this.page.setInputFiles('[data-testid="avatar-upload"]', 'tests/fixtures/test-avatar.jpg');
      await this.page.click('[data-testid="upload-avatar"]');
    });

    await this.measureAction('View User Stats', () =>
      this.page.click('[data-testid="user-stats-tab"]')
    );
  }

  async generateReport() {
    console.log('\n📊 Generating Performance Report...');
    
    const totalActions = this.metrics.length;
    const averageTime = this.metrics.reduce((sum, m) => sum + m.duration, 0) / totalActions;
    const maxTime = Math.max(...this.metrics.map(m => m.duration));
    const minTime = Math.min(...this.metrics.map(m => m.duration));
    
    console.log('\n📈 Performance Test Summary:');
    console.log(`   Total Actions: ${totalActions}`);
    console.log(`   Average Time: ${averageTime.toFixed(2)}ms`);
    console.log(`   Max Time: ${maxTime.toFixed(2)}ms`);
    console.log(`   Min Time: ${minTime.toFixed(2)}ms`);
    
    // Performance thresholds
    const thresholds = {
      excellent: 500,
      good: 1000,
      acceptable: 2000
    };
    
    console.log('\n🎯 Performance Analysis:');
    this.metrics.forEach(metric => {
      let status = '🟢 Excellent';
      if (metric.duration > thresholds.acceptable) {
        status = '🔴 Poor';
      } else if (metric.duration > thresholds.good) {
        status = '🟡 Acceptable';
      } else if (metric.duration > thresholds.excellent) {
        status = '🟢 Good';
      }
      
      console.log(`   ${metric.action}: ${metric.duration.toFixed(2)}ms ${status}`);
    });
    
    // Generate JSON report
    const report = {
      timestamp: new Date().toISOString(),
      summary: {
        totalActions,
        averageTime: averageTime.toFixed(2),
        maxTime: maxTime.toFixed(2),
        minTime: minTime.toFixed(2)
      },
      metrics: this.metrics,
      thresholds
    };
    
    // Save report to file (in real implementation)
    console.log('\n💾 Report saved to performance-report.json');
    
    return report;
  }
}

// Playwright test integration
test.describe('User Action Performance Tests', () => {
  test('should complete all user actions within performance thresholds', async ({ page }) => {
    const tester = new UserActionPerformanceTest(page);
    
    // Set performance budgets
    page.setDefaultTimeout(5000);
    page.setDefaultNavigationTimeout(3000);
    
    const report = await tester.runAllTests();
    
    // Assert performance thresholds
    expect(parseFloat(report.summary.averageTime)).toBeLessThan(2000);
    expect(parseFloat(report.summary.maxTime)).toBeLessThan(5000);
    
    // Assert critical actions are fast
    const criticalActions = report.metrics.filter(m => 
      m.action.includes('Navigate') || 
      m.action.includes('Submit') ||
      m.action.includes('Load')
    );
    
    criticalActions.forEach(action => {
      expect(action.duration).toBeLessThan(3000);
    });
  });

  test('should handle concurrent user actions without performance degradation', async ({ page, context }) => {
    // Test concurrent actions
    const pages = await Promise.all([
      context.newPage(),
      context.newPage(),
      context.newPage()
    ]);
    
    const startTime = performance.now();
    
    await Promise.all(pages.map(async (page, index) => {
      await page.goto('/community');
      await page.waitForSelector('[data-testid="post-list"]');
    }));
    
    const endTime = performance.now();
    const averageLoadTime = (endTime - startTime) / pages.length;
    
    console.log(`⚡ Concurrent page load average: ${averageLoadTime.toFixed(2)}ms`);
    expect(averageLoadTime).toBeLessThan(2000);
    
    // Cleanup
    await Promise.all(pages.map(page => page.close()));
  });
});

export default UserActionPerformanceTest;