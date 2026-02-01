import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import Login from '../views/Login.vue'

const history = createWebHistory();

const routes: Array<RouteRecordRaw> = [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue')
    }
  ]

  const router = createRouter({
    history: createWebHistory(),
    routes,
  });

export default router