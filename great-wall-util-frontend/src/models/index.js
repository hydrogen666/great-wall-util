import { notification } from 'antd';
import _ from 'lodash'
import { delay } from 'dva/saga'

async function postJson(url, data) {
  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
  },
    body: JSON.stringify(data),
  });
  if (resp.status !== 200) {
    throw resp;
  }
  return resp.json();
}

async function getJson(url) {
  const resp = await fetch(url);
  if (resp.status !== 200) {
    throw resp;
  }
  return resp.json();
}

const notify = (msg) => {
  notification.open({
    message: msg,
  });
}

export default {
  namespace: 'index',
  state: {
    config: {
    },
    instanceList: [],
    instance: '',
    deployMessage: [],
  },
  effects: {
    *init(ingore, { put, call }) {
      yield put({
        type: 'listInstance'
      });
      yield put({
        type: 'getMessageLoop'
      })
    },
    *listInstance(ignore, { put, call }) {
      try {
        const resp = yield call(getJson, '/api/list_instance');
        yield put({
          type: 'instanceList',
          payload: resp,
        })
      } catch (e) {
        notify('列出实例失败');
        console.log(e);
      }
    },
    *saveConfig({ payload }, { put, call }) {
      try {
        yield call(postJson, '/api/save_config', payload.config);
        yield put({
          type: 'getConfig',
          payload: {
            form: payload.form,
          }
        });
        notify('保存成功');
      } catch (e) {
        notify('保存失败');
        console.log(e);
      }
    },
    *getConfig({ payload }, { put, call }) {
      try {
        const config = yield call(getJson, '/api/get_config');
        payload.form.setFieldsValue(config);
      } catch (e) {
        notify('获取配置失败')
        console.log(e);
      }
    },
    *getMessageLoop(ignore, {put, call}) {
      let done = false;
      while (!done) {
        const message = yield call(getJson, '/api/get_deploy_message');
        yield put({
          type: 'deployMessage',
          payload: message,
        })
        if (message.length != 0 && message[message.length - 1].deployStep === 'DONE') {
          yield put({
            type: 'listInstance'
          })
          return;
        }
        yield call(delay, 100);
      }
    },
    *initDeploy({ payload }, { put, call }) {
      try {
        const { success } = yield call(postJson, '/api/init_deploy', payload);
        if (!success) {
          notify('已经有创建任务在执行')
        } else {
          notify('创建任务启动成功')
        }
        yield put({
          type: 'getMessageLoop',
        })
      } catch (e) {
        notify('接口调用失败')
        console.log(e)
      }
    },
    *destroyInstance({ payload }, { put, call }) {
      try {
        yield call(postJson, '/api/destroy_instance', payload)
        notify('销毁成功')
        yield put({
          type: 'listInstance'
        })
      } catch (e) {
        notify('销毁失败')
      }
    }
  },
  reducers: {
    instanceList(state, { payload }) {
      return {
        ...state,
        instanceList: payload,
        instance: _.size(payload) === 0 ? '' : _.keys(payload)[0],
      }
    },
    instance(state, {payload}) {
      return {
        ...state,
        instance: payload,
      }
    },
    deployMessage(state, {payload}) {
      return {
        ...state,
        deployMessage: payload
      }
    }
  },
}