<template>
  <div class="dashboard">
    <div class="stat-grid">
      <div
        v-for="(item, key) in metricConfigs"
        :key="key"
        class="stat-card"
        :class="[item.themeClass, { active: activeMetric === key }]"
        @click="toggleMetric(key)"
      >
        <div class="stat-icon">{{ item.icon }}</div>
        <div class="stat-value">{{ formatNumber(getMetricValue(key)) }}</div>
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-hint" v-if="item.hint">{{ item.hint }}</div>
        <div class="stat-variety-tags" v-if="getTopVarieties(key).length > 0">
          <span class="mini-tag" v-for="v in getTopVarieties(key)" :key="v.varietyName">
            {{ v.varietyName }}
          </span>
        </div>
      </div>
    </div>

    <div class="metric-detail card" v-if="activeMetric && activeMetricData">
      <div class="card-header">
        <h3 class="card-title">
          {{ metricConfigs[activeMetric].icon }} {{ metricConfigs[activeMetric].label }} — 详细分析
        </h3>
        <span class="close-btn" @click="activeMetric = null">×</span>
      </div>

      <div v-if="activeMetric === 'abnormalHealth'" class="health-aggregation-container">
        <div class="health-summary-bar">
          <div class="health-summary-item">
            <span class="summary-label">总记录数</span>
            <span class="summary-value">{{ activeMetricData.totalTrackingCount || 0 }}</span>
          </div>
          <div class="health-summary-item normal">
            <span class="summary-label">正常</span>
            <span class="summary-value">{{ activeMetricData.normalCount || 0 }}</span>
          </div>
          <div class="health-summary-item abnormal">
            <span class="summary-label">异常</span>
            <span class="summary-value">{{ activeMetricData.count || 0 }}</span>
          </div>
          <div class="health-summary-item unknown">
            <span class="summary-label">未知</span>
            <span class="summary-value">{{ activeMetricData.unknownCount || 0 }}</span>
          </div>
          <div class="health-summary-item rate">
            <span class="summary-label">异常率</span>
            <span class="summary-value">{{ formatPercent(activeMetricData.abnormalRate) }}</span>
          </div>
        </div>

        <div class="health-tabs">
          <div
            class="health-tab"
            :class="{ active: healthTab === 'variety' }"
            @click="healthTab = 'variety'"
          >
            按品种
          </div>
          <div
            class="health-tab"
            :class="{ active: healthTab === 'stage' }"
            @click="healthTab = 'stage'"
          >
            按阶段
          </div>
          <div
            class="health-tab"
            :class="{ active: healthTab === 'batch' }"
            @click="healthTab = 'batch'"
          >
            按批次
          </div>
          <div
            class="health-tab"
            :class="{ active: healthTab === 'details' }"
            @click="healthTab = 'details'"
          >
            详细记录
          </div>
        </div>

        <div v-show="healthTab === 'variety'" class="health-tab-content">
          <h4 class="section-title">📋 按品种统计</h4>
          <div class="variety-grid">
            <div
              v-for="v in activeMetricData.varieties"
              :key="v.varietyName"
              class="variety-card"
              :class="{ active: activeVariety === v.varietyName }"
              @click="activeVariety = activeVariety === v.varietyName ? null : v.varietyName"
            >
              <div class="variety-name">{{ v.varietyName }}</div>
              <div class="variety-metrics">
                <span class="variety-metric">
                  {{ v.trackingCount || 0 }} 条记录
                </span>
                <span class="variety-metric danger">
                  {{ v.issueCount || 0 }} 项异常
                </span>
                <span class="variety-metric warning">
                  异常率 {{ formatPercent(v.abnormalRate) }}
                </span>
              </div>
              <div class="health-tags" v-if="v.healthStatuses && v.healthStatuses.length > 0">
                <span class="health-tag" v-for="(h, i) in v.healthStatuses" :key="i">
                  {{ h }}
                </span>
              </div>
            </div>
            <div v-if="!activeMetricData.varieties || activeMetricData.varieties.length === 0" class="empty-state small">
              <div class="empty-text">暂无品种数据</div>
            </div>
          </div>
        </div>

        <div v-show="healthTab === 'stage'" class="health-tab-content">
          <h4 class="section-title">📊 按阶段统计</h4>
          <div class="stage-health-list">
            <div
              v-for="s in activeMetricData.byStage"
              :key="s.stageCode"
              class="stage-health-item"
            >
              <div class="stage-health-header">
                <span class="stage-name">{{ s.stageName }}</span>
                <span class="stage-count">{{ s.trackingCount || 0 }} 条记录</span>
              </div>
              <div class="stage-health-bar">
                <div
                  class="stage-health-fill"
                  :style="{ width: (s.trackingCount > 0 ? (s.abnormalCount / s.trackingCount * 100) : 0) + '%' }"
                  :class="{ 'has-abnormal': s.abnormalCount > 0 }"
                ></div>
              </div>
              <div class="stage-health-footer">
                <span class="abnormal-count">异常 {{ s.abnormalCount || 0 }}</span>
                <span class="abnormal-rate">异常率 {{ formatPercent(s.abnormalRate) }}</span>
              </div>
              <div v-if="s.commonAbnormalTypes && s.commonAbnormalTypes.length > 0" class="common-abnormal-tags">
                <span class="abnormal-type-tag" v-for="(t, i) in s.commonAbnormalTypes.slice(0, 3)" :key="i">
                  {{ t }}
                </span>
              </div>
            </div>
            <div v-if="!activeMetricData.byStage || activeMetricData.byStage.length === 0" class="empty-state small">
              <div class="empty-text">暂无阶段数据</div>
            </div>
          </div>
        </div>

        <div v-show="healthTab === 'batch'" class="health-tab-content">
          <h4 class="section-title">🪴 异常批次列表</h4>
          <div class="batch-health-list">
            <div
              v-for="b in activeMetricData.byBatch"
              :key="b.sowingId"
              class="batch-health-item"
              @click="goToGrowth(b.sowingId)"
            >
              <div class="batch-health-header">
                <span class="batch-variety">{{ b.varietyName }}</span>
                <span class="batch-stage">{{ b.currentStageName }}</span>
              </div>
              <div class="batch-health-body">
                <div class="batch-metric">
                  <span class="batch-metric-label">生长天数</span>
                  <span class="batch-metric-value">{{ b.daysSinceSowing || 0 }} 天</span>
                </div>
                <div class="batch-metric">
                  <span class="batch-metric-label">记录数</span>
                  <span class="batch-metric-value">{{ b.trackingCount || 0 }}</span>
                </div>
                <div class="batch-metric danger">
                  <span class="batch-metric-label">异常数</span>
                  <span class="batch-metric-value">{{ b.abnormalCount || 0 }}</span>
                </div>
              </div>
              <div class="batch-health-footer">
                <span class="latest-health">最新状态：{{ b.latestHealthStatus || '未知' }}</span>
              </div>
            </div>
            <div v-if="!activeMetricData.byBatch || activeMetricData.byBatch.length === 0" class="empty-state small">
              <div class="empty-text">暂无异常批次</div>
            </div>
          </div>
        </div>

        <div v-show="healthTab === 'details'" class="health-tab-content">
          <h4 class="section-title">📝 异常详情</h4>
          <div class="detail-list">
            <div
              v-for="d in filteredAbnormalDetails"
              :key="d.id"
              class="detail-item"
            >
              <div class="detail-main">
                <div class="detail-name">{{ d.varietyName }}</div>
                <div class="detail-sub">
                  {{ d.stageName || '' }} · {{ formatDate(d.recordTime) }}
                </div>
                <div class="detail-tags" v-if="d.abnormalType">
                  <span class="detail-tag" :class="'severity-' + (d.severityLevel || 1)">
                    {{ d.abnormalType }}
                  </span>
                </div>
              </div>
              <div class="detail-right">
                <div class="detail-badge abnormalHealth">
                  {{ d.healthStatus || '异常' }}
                </div>
              </div>
            </div>
            <div v-if="filteredAbnormalDetails.length === 0" class="empty-state small">
              <div class="empty-text">暂无记录</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="detail-container">
        <div class="variety-section">
          <h4 class="section-title">📋 关联品种</h4>
          <div class="variety-grid">
            <div
              v-for="v in activeMetricData.varieties"
              :key="v.varietyName"
              class="variety-card"
              :class="{ active: activeVariety === v.varietyName }"
              @click="activeVariety = activeVariety === v.varietyName ? null : v.varietyName"
            >
              <div class="variety-name">{{ v.varietyName }}</div>
              <div class="variety-metrics">
                <span class="variety-metric" v-if="v.totalQty != null">
                  {{ v.totalQty }} 粒
                </span>
                <span class="variety-metric" v-if="v.seedCount != null">
                  {{ v.seedCount }} 条
                </span>
                <span class="variety-metric" v-if="v.batchCount != null">
                  {{ v.batchCount }} 批
                </span>
                <span class="variety-metric" v-if="v.issueCount != null">
                  {{ v.issueCount }} 项
                </span>
                <span class="variety-metric danger" v-if="v.daysLeft != null || v.minDaysLeft != null">
                  ⏰ {{ v.daysLeft != null ? v.daysLeft : v.minDaysLeft }}天
                </span>
                <span class="variety-metric warning" v-if="v.maxDaysSince != null">
                  ⏳ {{ v.maxDaysSince }}天
                </span>
              </div>
              <div class="health-tags" v-if="v.healthStatuses && v.healthStatuses.length > 0">
                <span class="health-tag" v-for="(h, i) in v.healthStatuses" :key="i">
                  {{ h }}
                </span>
              </div>
            </div>
            <div v-if="!activeMetricData.varieties || activeMetricData.varieties.length === 0" class="empty-state small">
              <div class="empty-text">暂无关联品种</div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">📝 详细记录</h4>
          <div class="detail-list">
            <div
              v-for="d in filteredDetails"
              :key="d.id"
              class="detail-item"
            >
              <div class="detail-main">
                <div class="detail-name">{{ getDetailName(d) }}</div>
                <div class="detail-sub" v-if="getDetailSub(d)">
                  {{ getDetailSub(d) }}
                </div>
              </div>
              <div class="detail-right">
                <div class="detail-meta" v-if="getDetailMeta(d)">
                  {{ getDetailMeta(d) }}
                </div>
                <div class="detail-badge" :class="activeMetric">
                  {{ getDetailBadge(d) }}
                </div>
              </div>
            </div>
            <div v-if="filteredDetails.length === 0" class="empty-state small">
              <div class="empty-text">暂无记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="germination-swimlane card">
      <div class="card-header">
        <h3 class="card-title">🌱 发芽进度泳道</h3>
      </div>
      <div class="swimlane-container">
        <div
          v-for="lane in germinationLanes"
          :key="lane.laneKey"
          class="swimlane-column"
        >
          <div class="lane-header" :class="lane.laneKey">
            <span class="lane-icon">{{ laneIcons[lane.laneKey] }}</span>
            <span class="lane-name">{{ lane.laneName }}</span>
            <span class="lane-count">{{ lane.cards.length }}</span>
          </div>
          <div class="lane-cards">
            <div
              v-for="card in lane.cards"
              :key="card.sowingId"
              class="germ-card"
              @click="goToGrowth(card.sowingId)"
            >
              <div class="germ-card-header">
                <span class="germ-variety">{{ card.varietyName }}</span>
                <span class="germ-stage-tag" :class="lane.laneKey">{{ card.currentStageName }}</span>
              </div>
              <div class="germ-card-body">
                <div class="germ-info-row">
                  <span class="germ-info-label">播种数量</span>
                  <span class="germ-info-value quantity">{{ card.sowingQuantity }} 粒</span>
                </div>
                <div class="germ-info-row">
                  <span class="germ-info-label">环境温湿度</span>
                  <span class="germ-info-value env">
                    {{ card.temperature != null ? card.temperature + '°C' : '--' }}
                    /
                    {{ card.humidity != null ? card.humidity + '%' : '--' }}
                  </span>
                </div>
                <div class="germ-info-row">
                  <span class="germ-info-label">最近观察</span>
                  <span class="germ-info-value time">
                    {{ card.latestObservationTime ? formatDateTime(card.latestObservationTime) : '暂无记录' }}
                  </span>
                </div>
              </div>
              <div class="germ-card-footer">
                <span class="germ-sowing-time">播种于 {{ formatDate(card.sowingTime) }}</span>
              </div>
            </div>
            <div v-if="lane.cards.length === 0" class="lane-empty">
              <span>暂无数据</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="growth-timeline-section card">
      <div class="card-header">
        <h3 class="card-title">🌿 生长阶段时间轴</h3>
        <div class="timeline-selector">
          <el-select
            v-model="selectedTimelineSowingId"
            placeholder="选择播种记录"
            filterable
            size="small"
            style="width: 240px;"
          >
            <el-option
              v-for="t in growthTimelines"
              :key="t.sowingId"
              :label="`${t.varietyName} - ${formatDate(t.sowingTime)}`"
              :value="t.sowingId"
            />
          </el-select>
        </div>
      </div>
      <div v-if="selectedTimeline" class="timeline-summary">
        <div class="summary-item">
          <span class="summary-label">品种</span>
          <span class="summary-value">{{ selectedTimeline.varietyName }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">播种数量</span>
          <span class="summary-value">{{ selectedTimeline.sowingQuantity }} 粒</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">已生长</span>
          <span class="summary-value primary">{{ selectedTimeline.daysSinceSowing }} 天</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">当前阶段</span>
          <span class="summary-value success">{{ selectedTimeline.currentStageName }}</span>
        </div>
        <div class="summary-item" v-if="selectedTimeline.latestPlantHeight">
          <span class="summary-label">株高</span>
          <span class="summary-value">{{ selectedTimeline.latestPlantHeight }} cm</span>
        </div>
        <div class="summary-item" v-if="selectedTimeline.latestLeafCount">
          <span class="summary-label">叶片数</span>
          <span class="summary-value">{{ selectedTimeline.latestLeafCount }} 片</span>
        </div>
      </div>
      <div class="timeline-container">
        <div v-if="selectedTimeline && selectedTimeline.events" class="vertical-timeline">
          <div
            v-for="(event, index) in selectedTimeline.events"
            :key="index"
            class="timeline-node"
            :class="{ 'is-first': index === 0, 'is-last': index === selectedTimeline.events.length - 1 }"
          >
            <div class="timeline-dot" :class="getTimelineDotClass(event.stageCode)"></div>
            <div class="timeline-line" v-if="index < selectedTimeline.events.length - 1"></div>
            <div class="timeline-content-card">
              <div class="timeline-card-header">
                <span class="timeline-stage-tag" :class="event.stageCode">{{ event.stageName }}</span>
                <span class="timeline-time">{{ formatDateTime(event.recordTime) }}</span>
              </div>
              <div class="timeline-card-body">
                <div class="timeline-metrics">
                  <span v-if="event.plantHeight" class="t-metric">
                    <span class="t-metric-icon">📏</span>
                    <span class="t-metric-label">株高</span>
                    <span class="t-metric-value">{{ event.plantHeight }} cm</span>
                  </span>
                  <span v-if="event.leafCount" class="t-metric">
                    <span class="t-metric-icon">🍃</span>
                    <span class="t-metric-label">叶片数</span>
                    <span class="t-metric-value">{{ event.leafCount }} 片</span>
                  </span>
                  <span v-if="event.rootDevelopment" class="t-metric">
                    <span class="t-metric-icon">🌳</span>
                    <span class="t-metric-label">根系</span>
                    <span class="t-metric-value">{{ event.rootDevelopment }}</span>
                  </span>
                  <span v-if="event.healthStatus" class="t-metric">
                    <span class="t-metric-icon">💚</span>
                    <span class="t-metric-label">健康</span>
                    <span class="t-metric-value">{{ event.healthStatus }}</span>
                  </span>
                </div>
                <div v-if="event.temperature || event.humidity" class="timeline-env">
                  <span v-if="event.temperature">🌡️ {{ event.temperature }}°C</span>
                  <span v-if="event.humidity">💧 {{ event.humidity }}%</span>
                </div>
                <div v-if="event.notes" class="timeline-notes">{{ event.notes }}</div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">🌱</div>
          <div class="empty-text">请选择播种记录查看成长时间轴</div>
        </div>
      </div>
    </div>

    <div class="seed-calendar-section card">
      <div class="card-header">
        <h3 class="card-title">📅 种子活力日历</h3>
        <div class="calendar-controls">
          <button class="calendar-nav-btn" @click="prevCalendarMonth">‹</button>
          <span class="calendar-month-title">{{ calendarYear }}年{{ calendarMonth }}月</span>
          <button class="calendar-nav-btn" @click="nextCalendarMonth">›</button>
        </div>
      </div>
      <div class="calendar-legend">
        <span class="legend-item vital"><span class="legend-dot"></span>活力良好</span>
        <span class="legend-item expiring"><span class="legend-dot"></span>临近过期</span>
        <span class="legend-item expired"><span class="legend-dot"></span>已过期</span>
        <span class="legend-divider"></span>
        <span class="legend-stat">🌱 活力: {{ seedVitalityData?.vitalCount || 0 }}</span>
        <span class="legend-stat">⏰ 临期: {{ seedVitalityData?.expiringSoonCount || 0 }}</span>
        <span class="legend-stat">❌ 过期: {{ seedVitalityData?.expiredCount || 0 }}</span>
      </div>
      <div class="calendar-grid">
        <div class="calendar-weekday" v-for="day in weekDays" :key="day">{{ day }}</div>
        <div
          v-for="(day, index) in calendarDays"
          :key="index"
          class="calendar-day"
          :class="{
            'is-today': day.isToday,
            'is-other-month': day.isOtherMonth,
            'has-events': day.events && day.events.length > 0
          }"
        >
          <div class="day-number">{{ day.day }}</div>
          <div class="day-events" v-if="day.events && day.events.length > 0">
            <div
              v-for="(evt, evtIdx) in day.events.slice(0, 2)"
              :key="evtIdx"
              class="day-event"
              :class="evt.vitalityStatus"
              :title="`${evt.varietyName} - ${evt.storageLocation} - 剩余${evt.remainingQuantity}粒`"
            >
              <span class="event-name">{{ evt.varietyName }}</span>
              <span class="event-location">{{ evt.storageLocation }}</span>
            </div>
            <div v-if="day.events.length > 2" class="more-events">+{{ day.events.length - 2 }} 更多</div>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card recent-seeds">
        <div class="card-header">
          <h3 class="card-title">📦 最近种子入库</h3>
          <router-link to="/seeds" class="more-link">查看全部 →</router-link>
        </div>
        <div v-if="recentSeeds.length > 0" class="seed-list">
          <div v-for="seed in recentSeeds" :key="seed.id" class="seed-item">
            <div class="seed-name">{{ seed.varietyName }}</div>
            <div class="seed-meta">
              <span class="seed-source">{{ sourceTypeMap[seed.sourceType] || seed.sourceType }}</span>
              <span class="seed-qty">剩余 {{ seed.remainingQuantity }} 粒</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">🌱</div>
          <div class="empty-text">暂无种子记录</div>
        </div>
      </div>

      <div class="card recent-sowings">
        <div class="card-header">
          <h3 class="card-title">🌾 最近播种</h3>
          <router-link to="/sowings" class="more-link">查看全部 →</router-link>
        </div>
        <div v-if="recentSowings.length > 0" class="sowing-list">
          <div v-for="sowing in recentSowings" :key="sowing.id" class="sowing-item">
            <div class="sowing-name">{{ sowing.varietyName }}</div>
            <div class="sowing-meta">
              <span class="sowing-date">{{ formatDate(sowing.sowingTime) }}</span>
              <span class="sowing-qty">{{ sowing.sowingQuantity }} 粒</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">🌾</div>
          <div class="empty-text">暂无播种记录</div>
        </div>
      </div>

      <div class="card stages-card">
        <div class="card-header">
          <h3 class="card-title">📋 标准培育流程</h3>
        </div>
        <div class="timeline">
          <div v-for="stage in stages" :key="stage.id" class="timeline-item is-done">
            <div class="timeline-title">{{ stage.stageName }}</div>
            <div class="timeline-content">{{ stage.description }}</div>
          </div>
        </div>
      </div>

      <div class="card tips-card">
        <div class="card-header">
          <h3 class="card-title">💡 养护小贴士</h3>
        </div>
        <div class="tips-list">
          <div class="tip-item">
            <span class="tip-icon">💧</span>
            <span>播种初期保持土壤湿润，避免积水</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">☀️</span>
            <span>萌芽后逐渐增加光照，避免暴晒</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">🌡️</span>
            <span>最适发芽温度 20-25°C</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">🪴</span>
            <span>长出 2-3 片真叶后可进行移栽</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">🌿</span>
            <span>移栽后需缓苗一周，避免强光直射</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getSeedList } from '@/api/seed'
import { getSowingList } from '@/api/sowing'
import { getStageList } from '@/api/stage'
import { getDashboardStats, getGerminationProgress, getGrowthTimeline, getSeedVitalityCalendar } from '@/api/dashboard'
import dayjs from 'dayjs'
import { formatDate, formatDateTime } from '@/utils/date'

const router = useRouter()

const metricConfigs = {
  totalSeeds: {
    icon: '🌱',
    label: '种子总量',
    themeClass: 'stat-primary',
    hint: '粒',
    topVarietyKey: 'totalQty'
  },
  activeSowings: {
    icon: '🌾',
    label: '活跃播种批次',
    themeClass: 'stat-success',
    hint: '批',
    topVarietyKey: 'batchCount'
  },
  expiringSeeds: {
    icon: '⏰',
    label: '临期种子',
    themeClass: 'stat-warning',
    hint: '30天内到期',
    topVarietyKey: 'seedCount'
  },
  pendingTransplants: {
    icon: '🪴',
    label: '待移栽批次',
    themeClass: 'stat-info',
    hint: '可移栽',
    topVarietyKey: 'batchCount'
  },
  abnormalHealth: {
    icon: '⚠️',
    label: '异常健康状态',
    themeClass: 'stat-danger',
    hint: '需关注',
    topVarietyKey: 'issueCount'
  }
}

const stats = reactive({
  totalSeeds: { count: 0, varieties: [], details: [] },
  activeSowings: { count: 0, varieties: [], details: [] },
  expiringSeeds: { count: 0, varieties: [], details: [] },
  pendingTransplants: { count: 0, varieties: [], details: [] },
  abnormalHealth: { count: 0, varieties: [], details: [] }
})

const seedList = ref([])
const sowingList = ref([])
const stages = ref([])

const activeMetric = ref(null)
const activeVariety = ref(null)
const healthTab = ref('variety')

const germinationLanes = ref([])
const laneIcons = {
  PENDING_GERMINATION: '🟡',
  SPROUTING: '🟢',
  LEAFING: '🌿',
  ACCLIMATING: '🪴'
}

const growthTimelines = ref([])
const selectedTimelineSowingId = ref(null)
const seedVitalityData = ref(null)
const calendarYear = ref(dayjs().year())
const calendarMonth = ref(dayjs().month() + 1)
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const selectedTimeline = computed(() => {
  if (!selectedTimelineSowingId.value || !growthTimelines.value.length) return null
  return growthTimelines.value.find(t => t.sowingId === selectedTimelineSowingId.value)
})

const calendarDays = computed(() => {
  const days = []
  const year = calendarYear.value
  const month = calendarMonth.value
  const today = dayjs()

  const firstDay = dayjs(`${year}-${month}-01`)
  const startWeekDay = firstDay.day()
  const daysInMonth = firstDay.daysInMonth()

  const prevMonth = firstDay.subtract(1, 'month')
  const prevMonthDays = prevMonth.daysInMonth()

  for (let i = startWeekDay - 1; i >= 0; i--) {
    days.push({
      day: prevMonthDays - i,
      isOtherMonth: true,
      isToday: false,
      date: prevMonth.date(prevMonthDays - i).format('YYYY-MM-DD'),
      events: []
    })
  }

  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = dayjs(`${year}-${month}-${i}`).format('YYYY-MM-DD')
    const isToday = today.format('YYYY-MM-DD') === dateStr
    days.push({
      day: i,
      isOtherMonth: false,
      isToday,
      date: dateStr,
      events: getEventsForDate(dateStr)
    })
  }

  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    const nextMonth = firstDay.add(1, 'month')
    days.push({
      day: i,
      isOtherMonth: true,
      isToday: false,
      date: nextMonth.date(i).format('YYYY-MM-DD'),
      events: []
    })
  }

  return days
})

const getEventsForDate = (dateStr) => {
  if (!seedVitalityData.value || !seedVitalityData.value.dateEvents) return []
  return seedVitalityData.value.dateEvents[dateStr] || []
}

const recentSeeds = computed(() => seedList.value.slice(0, 5))
const recentSowings = computed(() => sowingList.value.slice(0, 5))

const sourceTypeMap = {
  PURCHASE: '购入',
  HARVEST: '采收'
}

const activeMetricData = computed(() => {
  if (!activeMetric.value) return null
  return stats[activeMetric.value]
})

const filteredDetails = computed(() => {
  if (!activeMetricData.value || !activeMetricData.value.details) return []
  const details = activeMetricData.value.details
  if (!activeVariety.value) return details
  return details.filter(d => d.varietyName === activeVariety.value)
})

const filteredAbnormalDetails = computed(() => {
  if (!activeMetricData.value || !activeMetricData.value.details) return []
  const details = activeMetricData.value.details
  if (!activeVariety.value) return details
  return details.filter(d => d.varietyName === activeVariety.value)
})

const formatNumber = (n) => {
  if (n == null) return 0
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toLocaleString()
}

const formatPercent = (value) => {
  if (value == null || isNaN(value)) return '0%'
  return Number(value).toFixed(1) + '%'
}



const goToGrowth = (sowingId) => {
  router.push({ path: '/growth', query: { sowingId } })
}

const getMetricValue = (key) => {
  return stats[key]?.count ?? 0
}

const getTopVarieties = (key) => {
  const varieties = stats[key]?.varieties || []
  return varieties.slice(0, 3)
}

const toggleMetric = (key) => {
  activeMetric.value = activeMetric.value === key ? null : key
  activeVariety.value = null
  if (key === 'abnormalHealth') {
    healthTab.value = 'variety'
  }
}

const getDetailName = (d) => d.varietyName

const getDetailSub = (d) => {
  const key = activeMetric.value
  if (key === 'totalSeeds') {
    return `${sourceTypeMap[d.sourceType] || d.sourceType || ''} · 入库 ${d.acquireTime || ''}`
  }
  if (key === 'activeSowings' || key === 'pendingTransplants') {
    return `播种 ${formatDate(d.sowingTime)} · 已${d.daysSince != null ? d.daysSince + '天' : ''}`
  }
  if (key === 'expiringSeeds') {
    return `到期 ${d.expireDate || ''}`
  }
  if (key === 'abnormalHealth') {
    return `${d.stageName || ''} · 记录 ${formatDate(d.recordTime)}`
  }
  return ''
}

const getDetailMeta = (d) => {
  const key = activeMetric.value
  if (key === 'totalSeeds') {
    return `剩余 ${d.remainingQuantity} 粒`
  }
  if (key === 'activeSowings' || key === 'pendingTransplants') {
    if (d.remainingQuantity != null && d.transplantedQuantity != null) {
      return `剩余 ${d.remainingQuantity} / 播种 ${d.sowingQuantity} 粒（已移栽 ${d.transplantedQuantity}）`
    }
    return `${d.sowingQuantity} 粒`
  }
  if (key === 'expiringSeeds') {
    return `剩余 ${d.remainingQuantity} 粒`
  }
  return ''
}

const getDetailBadge = (d) => {
  const key = activeMetric.value
  if (key === 'expiringSeeds') {
    const dl = d.daysLeft
    if (dl < 0) return `已过期${Math.abs(dl)}天`
    if (dl === 0) return '今天到期'
    return `剩${dl}天`
  }
  if (key === 'abnormalHealth') {
    return d.healthStatus || '异常'
  }
  if (key === 'pendingTransplants') {
    return '待移栽'
  }
  if (key === 'activeSowings') {
    return '培育中'
  }
  if (key === 'totalSeeds') {
    return '在库'
  }
  return ''
}



const getTimelineDotClass = (stageCode) => {
  if (!stageCode) return 'stage-default'
  const stageMap = {
    SOWN: 'stage-sown',
    PENDING_GERMINATION: 'stage-pending',
    SPROUTING: 'stage-sprouting',
    COTYLEDON: 'stage-sprouting',
    TRUE_LEAF: 'stage-leafing',
    LEAFING: 'stage-leafing',
    SEEDLING: 'stage-leafing',
    ROOT_DEVELOPED: 'stage-leafing',
    TRANSPLANTED: 'stage-transplant',
    ACCLIMATING: 'stage-transplant',
    GROWING: 'stage-growing',
    FLOWERING: 'stage-flowering'
  }
  return stageMap[stageCode] || 'stage-default'
}

const prevCalendarMonth = () => {
  if (calendarMonth.value === 1) {
    calendarMonth.value = 12
    calendarYear.value--
  } else {
    calendarMonth.value--
  }
}

const nextCalendarMonth = () => {
  if (calendarMonth.value === 12) {
    calendarMonth.value = 1
    calendarYear.value++
  } else {
    calendarMonth.value++
  }
}

const loadData = async () => {
  try {
    const [seeds, sowings, stageList, dashboardStats, germinationData, timelineData, vitalityData] = await Promise.all([
      getSeedList(),
      getSowingList(),
      getStageList(),
      getDashboardStats(),
      getGerminationProgress(),
      getGrowthTimeline(),
      getSeedVitalityCalendar()
    ])
    seedList.value = seeds || []
    sowingList.value = sowings || []
    stages.value = stageList || []
    if (dashboardStats) {
      Object.keys(metricConfigs).forEach(key => {
        if (dashboardStats[key]) {
          stats[key] = dashboardStats[key]
        }
      })
    }
    if (germinationData) {
      germinationLanes.value = germinationData
    }
    if (timelineData) {
      growthTimelines.value = timelineData
      if (timelineData.length > 0 && !selectedTimelineSowingId.value) {
        selectedTimelineSowingId.value = timelineData[0].sowingId
      }
    }
    if (vitalityData) {
      seedVitalityData.value = vitalityData
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.germination-swimlane {
  margin-bottom: 24px;
}

.swimlane-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  overflow-x: auto;
}

@media (max-width: 900px) {
  .swimlane-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .swimlane-container {
    grid-template-columns: 1fr;
  }
}

.swimlane-column {
  background: #fafbfc;
  border-radius: 8px;
  min-width: 0;
}

.lane-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 8px 8px 0 0;
  font-size: 14px;
  font-weight: 600;
  color: white;
}

.lane-header.PENDING_GERMINATION {
  background: linear-gradient(135deg, #e6a23c, #f5c96a);
}

.lane-header.SPROUTING {
  background: linear-gradient(135deg, #67c23a, #95d475);
}

.lane-header.LEAFING {
  background: linear-gradient(135deg, #409eff, #79bbff);
}

.lane-header.ACCLIMATING {
  background: linear-gradient(135deg, #5ac8fa, #95d4f5);
}

.lane-icon {
  font-size: 16px;
}

.lane-name {
  flex: 1;
}

.lane-count {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  padding: 1px 8px;
  font-size: 12px;
  font-weight: 500;
}

.lane-cards {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
}

.germ-card {
  background: white;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid #dcdfe6;
}

.germ-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.germ-card:nth-child(4n+1) {
  border-left-color: #e6a23c;
}

.germ-card:nth-child(4n+2) {
  border-left-color: #67c23a;
}

.germ-card:nth-child(4n+3) {
  border-left-color: #409eff;
}

.germ-card:nth-child(4n) {
  border-left-color: #5ac8fa;
}

.germ-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.germ-variety {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}

.germ-stage-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  white-space: nowrap;
}

.germ-stage-tag.PENDING_GERMINATION {
  background: #fdf6ec;
  color: #e6a23c;
}

.germ-stage-tag.SPROUTING {
  background: #f0f9eb;
  color: #67c23a;
}

.germ-stage-tag.LEAFING {
  background: #ecf5ff;
  color: #409eff;
}

.germ-stage-tag.ACCLIMATING {
  background: #f0f9ff;
  color: #5ac8fa;
}

.germ-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.germ-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.germ-info-label {
  color: #909399;
}

.germ-info-value {
  font-weight: 500;
  color: #303133;
}

.germ-info-value.quantity {
  color: #e6a23c;
}

.germ-info-value.env {
  color: #409eff;
}

.germ-info-value.time {
  color: #67c23a;
}

.germ-card-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}

.germ-sowing-time {
  font-size: 11px;
  color: #c0c4cc;
}

.lane-empty {
  text-align: center;
  padding: 30px 10px;
  color: #c0c4cc;
  font-size: 13px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

.more-link {
  font-size: 13px;
  color: #67c23a;
  text-decoration: none;
  cursor: pointer;
}

.seed-list, .sowing-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.seed-item, .sowing-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  transition: background 0.3s;

  &:hover {
    background: #ecf5ff;
  }
}

.seed-name, .sowing-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.seed-meta, .sowing-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.seed-source, .sowing-date {
  color: #67c23a;
}

.seed-qty, .sowing-qty {
  color: #e6a23c;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;

  .tip-icon {
    font-size: 18px;
    flex-shrink: 0;
  }
}

.stat-card {
  cursor: pointer;
  position: relative;
  border: 2px solid transparent;

  &.active {
    border-color: currentColor;
    transform: translateY(-2px);
  }

  .stat-hint {
    font-size: 12px;
    color: #c0c4cc;
    margin-top: 4px;
  }

  .stat-variety-tags {
    margin-top: 10px;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
  }

  .mini-tag {
    display: inline-block;
    padding: 2px 8px;
    font-size: 11px;
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
    border-radius: 10px;
    max-width: 80px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

.stat-card.stat-primary .mini-tag {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}
.stat-card.stat-success .mini-tag {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}
.stat-card.stat-warning .mini-tag {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}
.stat-card.stat-info .mini-tag {
  background: rgba(90, 200, 250, 0.1);
  color: #5ac8fa;
}
.stat-card.stat-danger .mini-tag {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.stat-info .stat-value {
  color: #5ac8fa;
}

.metric-detail {
  margin-bottom: 24px;

  .close-btn {
    cursor: pointer;
    font-size: 24px;
    color: #c0c4cc;
    line-height: 1;
    padding: 0 8px;
    transition: color 0.2s;

    &:hover {
      color: #f56c6c;
    }
  }
}

.detail-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}

.variety-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.variety-card {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: #ecf5ff;
    border-color: #409eff33;
  }

  &.active {
    background: #ecf5ff;
    border-color: #409eff;
  }

  .variety-name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  .variety-metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .variety-metric {
    font-size: 11px;
    padding: 2px 6px;
    background: white;
    border-radius: 4px;
    color: #606266;

    &.danger {
      color: #f56c6c;
      background: #fef0f0;
    }

    &.warning {
      color: #e6a23c;
      background: #fdf6ec;
    }
  }

  .health-tags {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .health-tag {
    font-size: 11px;
    padding: 2px 6px;
    background: #fef0f0;
    color: #f56c6c;
    border-radius: 4px;
  }
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  transition: background 0.2s;

  &:hover {
    background: #f0f9eb;
  }

  .detail-main {
    flex: 1;
    min-width: 0;
  }

  .detail-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }

  .detail-sub {
    font-size: 12px;
    color: #909399;
  }

  .detail-right {
    text-align: right;
    flex-shrink: 0;
    margin-left: 12px;
  }

  .detail-meta {
    font-size: 13px;
    color: #67c23a;
    font-weight: 500;
    margin-bottom: 4px;
  }

  .detail-badge {
    display: inline-block;
    font-size: 11px;
    padding: 3px 8px;
    border-radius: 4px;
    background: #67c23a1a;
    color: #67c23a;

    &.expiringSeeds {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.abnormalHealth {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.pendingTransplants {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.activeSowings {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.totalSeeds {
      background: #ecf5ff;
      color: #409eff;
    }
  }
}

.empty-state {
  &.small {
    padding: 30px 20px;

    .empty-icon {
      font-size: 32px;
    }

    .empty-text {
      font-size: 13px;
    }
  }
}

.growth-timeline-section {
  margin-bottom: 24px;

  .timeline-selector {
    display: flex;
    align-items: center;
  }

  .timeline-summary {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 20px;

    .summary-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .summary-label {
        font-size: 12px;
        color: #909399;
      }

      .summary-value {
        font-size: 15px;
        font-weight: 600;
        color: #303133;

        &.primary {
          color: #409eff;
        }

        &.success {
          color: #67c23a;
        }
      }
    }
  }

  .timeline-container {
    max-height: 500px;
    overflow-y: auto;
    padding-right: 8px;
  }

  .vertical-timeline {
    position: relative;
    padding-left: 30px;
  }

  .timeline-node {
    position: relative;
    padding-bottom: 24px;

    &:last-child {
      padding-bottom: 0;
    }
  }

  .timeline-dot {
    position: absolute;
    left: -30px;
    top: 8px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background: #dcdfe6;
    border: 2px solid white;
    box-shadow: 0 0 0 2px #dcdfe6;
    z-index: 1;

    &.stage-sown {
      background: #e6a23c;
      box-shadow: 0 0 0 2px #e6a23c;
    }

    &.stage-pending {
      background: #f56c6c;
      box-shadow: 0 0 0 2px #f56c6c;
    }

    &.stage-sprouting {
      background: #67c23a;
      box-shadow: 0 0 0 2px #67c23a;
    }

    &.stage-leafing {
      background: #409eff;
      box-shadow: 0 0 0 2px #409eff;
    }

    &.stage-transplant {
      background: #5ac8fa;
      box-shadow: 0 0 0 2px #5ac8fa;
    }

    &.stage-growing {
      background: #13ce66;
      box-shadow: 0 0 0 2px #13ce66;
    }

    &.stage-flowering {
      background: #ff6b6b;
      box-shadow: 0 0 0 2px #ff6b6b;
    }

    &.stage-default {
      background: #909399;
      box-shadow: 0 0 0 2px #909399;
    }
  }

  .timeline-line {
    position: absolute;
    left: -24px;
    top: 22px;
    bottom: 0;
    width: 2px;
    background: #ebeef5;
  }

  .timeline-content-card {
    background: white;
    border-radius: 8px;
    padding: 14px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
    border-left: 3px solid #dcdfe6;
    transition: all 0.2s;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
  }

  .timeline-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }

  .timeline-stage-tag {
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 12px;
    font-weight: 500;
    background: #f0f9eb;
    color: #67c23a;

    &.SOWN {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.SPROUTING, &.COTYLEDON {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.TRUE_LEAF, &.LEAFING, &.SEEDLING, &.ROOT_DEVELOPED {
      background: #ecf5ff;
      color: #409eff;
    }

    &.TRANSPLANTED, &.ACCLIMATING {
      background: #f0f9ff;
      color: #5ac8fa;
    }

    &.GROWING {
      background: #f0f9eb;
      color: #13ce66;
    }

    &.FLOWERING {
      background: #fef0f0;
      color: #ff6b6b;
    }
  }

  .timeline-time {
    font-size: 12px;
    color: #909399;
  }

  .timeline-card-body {
    .timeline-metrics {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 8px;
    }

    .t-metric {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      background: #f5f7fa;
      border-radius: 16px;
      font-size: 12px;

      .t-metric-icon {
        font-size: 14px;
      }

      .t-metric-label {
        color: #909399;
      }

      .t-metric-value {
        color: #303133;
        font-weight: 500;
      }
    }

    .timeline-env {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: #909399;
      margin-bottom: 6px;
    }

    .timeline-notes {
      font-size: 12px;
      color: #606266;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 4px;
      margin-top: 6px;
    }
  }
}

.seed-calendar-section {
  margin-bottom: 24px;

  .calendar-controls {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .calendar-nav-btn {
    width: 28px;
    height: 28px;
    border: 1px solid #dcdfe6;
    background: white;
    border-radius: 4px;
    cursor: pointer;
    font-size: 16px;
    color: #606266;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
      color: #409eff;
    }
  }

  .calendar-month-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    min-width: 100px;
    text-align: center;
  }

  .calendar-legend {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px 0;
    margin-bottom: 12px;
    border-bottom: 1px solid #ebeef5;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #606266;

    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
    }

    &.vital .legend-dot {
      background: #67c23a;
    }

    &.expiring .legend-dot {
      background: #e6a23c;
    }

    &.expired .legend-dot {
      background: #f56c6c;
    }
  }

  .legend-divider {
    width: 1px;
    height: 16px;
    background: #ebeef5;
    margin: 0 4px;
  }

  .legend-stat {
    font-size: 12px;
    color: #606266;
  }

  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 2px;
    background: #ebeef5;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;
  }

  .calendar-weekday {
    background: #f5f7fa;
    padding: 10px;
    text-align: center;
    font-size: 13px;
    font-weight: 500;
    color: #606266;
  }

  .calendar-day {
    background: white;
    min-height: 80px;
    padding: 6px;
    position: relative;
    transition: background 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    &.is-other-month {
      background: #fafbfc;
      opacity: 0.5;
    }

    &.is-today {
      background: #ecf5ff;

      .day-number {
        background: #409eff;
        color: white;
        font-weight: 600;
      }
    }

    &.has-events {
      cursor: pointer;
    }
  }

  .day-number {
    width: 22px;
    height: 22px;
    line-height: 22px;
    text-align: center;
    font-size: 12px;
    color: #303133;
    border-radius: 50%;
    margin-bottom: 4px;
  }

  .day-events {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  .day-event {
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 3px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: flex;
    flex-direction: column;
    gap: 1px;

    &.VITAL {
      background: #f0f9eb;
      color: #67c23a;
      border-left: 2px solid #67c23a;
    }

    &.EXPIRING_SOON {
      background: #fdf6ec;
      color: #e6a23c;
      border-left: 2px solid #e6a23c;
    }

    &.EXPIRED {
      background: #fef0f0;
      color: #f56c6c;
      border-left: 2px solid #f56c6c;
    }

    .event-name {
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .event-location {
      font-size: 9px;
      opacity: 0.8;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .more-events {
    font-size: 10px;
    color: #909399;
    padding-left: 6px;
  }
}

@media (max-width: 768px) {
  .growth-timeline-section {
    .timeline-summary {
      gap: 12px;
    }

    .summary-item .summary-value {
      font-size: 13px;
    }
  }

  .seed-calendar-section {
    .calendar-day {
      min-height: 60px;
      padding: 4px;
    }

    .day-event {
      font-size: 9px;
      padding: 1px 4px;

      .event-location {
        display: none;
      }
    }
  }
}

.health-aggregation-container {
  .health-summary-bar {
    display: flex;
    gap: 12px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 16px;
    flex-wrap: wrap;
  }

  .health-summary-item {
    flex: 1;
    min-width: 80px;
    text-align: center;
    padding: 10px;
    background: white;
    border-radius: 6px;

    .summary-label {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }

    .summary-value {
      display: block;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    &.normal .summary-value {
      color: #67c23a;
    }

    &.abnormal .summary-value {
      color: #f56c6c;
    }

    &.unknown .summary-value {
      color: #909399;
    }

    &.rate .summary-value {
      color: #e6a23c;
    }
  }

  .health-tabs {
    display: flex;
    gap: 4px;
    margin-bottom: 16px;
    border-bottom: 2px solid #ebeef5;
  }

  .health-tab {
    padding: 10px 20px;
    font-size: 14px;
    color: #606266;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    transition: all 0.2s;

    &:hover {
      color: #409eff;
    }

    &.active {
      color: #409eff;
      border-bottom-color: #409eff;
      font-weight: 500;
    }
  }

  .health-tab-content {
    min-height: 200px;
  }

  .stage-health-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .stage-health-item {
    padding: 12px;
    background: #f5f7fa;
    border-radius: 8px;
    transition: all 0.2s;

    &:hover {
      background: #ecf5ff;
    }
  }

  .stage-health-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .stage-name {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
    }

    .stage-count {
      font-size: 12px;
      color: #909399;
    }
  }

  .stage-health-bar {
    height: 8px;
    background: #e4e7ed;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 8px;
  }

  .stage-health-fill {
    height: 100%;
    background: #67c23a;
    border-radius: 4px;
    transition: width 0.3s ease;

    &.has-abnormal {
      background: #f56c6c;
    }
  }

  .stage-health-footer {
    display: flex;
    justify-content: space-between;
    font-size: 12px;

    .abnormal-count {
      color: #f56c6c;
    }

    .abnormal-rate {
      color: #e6a23c;
    }
  }

  .common-abnormal-tags {
    display: flex;
    gap: 6px;
    margin-top: 8px;
    flex-wrap: wrap;
  }

  .abnormal-type-tag {
    font-size: 11px;
    padding: 2px 8px;
    background: #fef0f0;
    color: #f56c6c;
    border-radius: 10px;
  }

  .batch-health-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 12px;
  }

  .batch-health-item {
    padding: 12px;
    background: #f5f7fa;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    border-left: 3px solid #f56c6c;

    &:hover {
      background: #fef0f0;
      transform: translateY(-1px);
    }
  }

  .batch-health-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .batch-variety {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }

    .batch-stage {
      font-size: 11px;
      padding: 2px 8px;
      background: #ecf5ff;
      color: #409eff;
      border-radius: 10px;
    }
  }

  .batch-health-body {
    display: flex;
    gap: 10px;
    margin-bottom: 8px;
  }

  .batch-metric {
    flex: 1;
    text-align: center;

    .batch-metric-label {
      display: block;
      font-size: 11px;
      color: #909399;
      margin-bottom: 2px;
    }

    .batch-metric-value {
      font-size: 13px;
      font-weight: 500;
      color: #606266;
    }

    &.danger .batch-metric-value {
      color: #f56c6c;
    }
  }

  .batch-health-footer {
    .latest-health {
      font-size: 12px;
      color: #f56c6c;
    }
  }

  .detail-tags {
    margin-top: 6px;
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
  }

  .detail-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 10px;
    background: #fef0f0;
    color: #f56c6c;

    &.severity-1 {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.severity-2 {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.severity-3 {
      background: #f56c6c;
      color: white;
    }
  }
}
</style>
