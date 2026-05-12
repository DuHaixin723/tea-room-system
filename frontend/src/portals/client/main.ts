// 入口文件：负责初始化某一端口或某一角色的前端应用。

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { INIT_OPTIONS_KEY } from 'vue-echarts'
import '@/assets/styles/main.css'
import App from '@/App.vue'
import router from '@/portals/client/router'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.provide(INIT_OPTIONS_KEY, { renderer: 'canvas' })
app.mount('#app')
