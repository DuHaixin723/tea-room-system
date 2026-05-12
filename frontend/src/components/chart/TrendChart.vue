<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

import { computed } from 'vue'
import VChart from 'vue-echarts'

const props = defineProps<{
  title: string
  values: number[]
}>()

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLine: { lineStyle: { color: '#b9ac95' } },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(88,72,53,0.08)' } },
  },
  series: [
    {
      name: props.title,
      type: 'line',
      smooth: true,
      data: props.values,
      lineStyle: { color: '#7b5832', width: 3 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(123,88,50,0.32)' },
            { offset: 1, color: 'rgba(123,88,50,0.02)' },
          ],
        },
      },
      symbolSize: 8,
      itemStyle: { color: '#d7a255' },
    },
  ],
}))
</script>

<template>
  <VChart class="chart-card" :option="option" :init-options="{ renderer: 'canvas' }" autoresize />
</template>

