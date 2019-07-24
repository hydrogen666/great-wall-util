import styles from './index.css';
import React from 'react';
import { Button, Tabs, Row, Col, Input, Form, Select } from 'antd';
import { connect } from 'dva';
import _ from 'lodash';

import SimpleForm from '../components/SimpleForm'

const { TabPane } = Tabs;
const { TextArea } = Input;
const { Option } = Select;

const formItemLayout = {
  labelCol: { span: 8 },
  wrapperCol: { span: 10 },
};
const formButtonLayout = {
  labelCol: { span: 8 },
  wrapperCol: { span: 10, offset: 8 },
};

class Index extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      releaseAfterHour: 4,
    }
  }

  componentDidMount() {
    this.props.dispatch({
      type: 'index/init'
    });
  }

  getConfig = () => {
    this.props.dispatch({
      type: 'index/getConfig',
      payload: {
        form: this.configForm,
      }
    });
  }

  onSaveConfig = (config) => {
    this.props.dispatch({
      type: 'index/saveConfig',
      payload: {
        config,
        form: this.configForm,
      },
    })
  }

  onInstanceSelectChange = (instanceId) => {
    this.props.dispatch({
      type: 'index/instance',
      payload: instanceId,
    })
  }

  onReleaseAfterHourChange = (v) => {
    this.setState({
      releaseAfterHour: v,
    })
  }

  onInitDeploy = () => {
    this.props.dispatch({
      type: 'index/initDeploy',
      payload: {
        releaseAfterHour: this.state.releaseAfterHour,
      }
    })
  }
  
  onDestroyInstance = () => {
    this.props.dispatch({
      type: 'index/destroyInstance',
      payload: {
        instanceId: this.props.instance
      }
    })
  }

  onDeployBrookServer = () => {
    this.props.dispatch({
      type: 'index/deployBrookServer',
      payload: {
        instanceId: this.props.instance
      }
    })
  }

  onDeployBrookClient = () => {
    this.props.dispatch({
      type: 'index/deployBrookClient',
      payload: {
        instanceId: this.props.instance
      }
    })
  }

  render() {
    const selectedInstance = _.get(this.props.instanceList, this.props.instance, {});

    let allMsg = ''

    _.forEach(this.props.deployMessage, (msg) => { 
      allMsg += `${msg.msg}\n`
    })
    return (
      <div>
        <Tabs>
          <TabPane
            tab="部署"
            key="1"
          >
            <Row gutter={16}>
              <Col span={12}>
                <Form {...formItemLayout}
                  style={{
                    paddingLeft: 16
                  }}
                >
                  <Form.Item label="实例">
                    <Select value={this.props.instance} onChange={this.onInstanceSelectChange}>
                      {
                        _.map(this.props.instanceList, (instance) => {
                          const { instanceId } = instance;
                          return (
                            <Option key={instanceId}>
                                {
                                  `${instanceId}`
                                }
                            </Option>
                          )
                        })
                      }
                    </Select>
                  </Form.Item>
                  <Form.Item label="实例详情">
                    <div>
                      {`${selectedInstance.instanceId || ''}`}
                    </div>
                    <div>
                      {
                        _.reduce(selectedInstance.publicIp, (concat, ip) => {
                          if (concat) {
                            return concat + ',' + ip;
                          } else {
                            return ip
                          }
                        })
                      }
                    </div>
                  </Form.Item>
                  <Form.Item label="用几小时">
                    <Select value={this.state.releaseAfterHour} onChange={this.onReleaseAfterHourChange}>
                      <Option key={1}>1</Option>
                      <Option key={2}>2</Option>
                      <Option key={3}>3</Option>
                      <Option key={4}>4</Option>
                      <Option key={5}>5</Option>
                      <Option key={6}>6</Option>
                    </Select>
                  </Form.Item>
                  <Form.Item {...formButtonLayout}>
                    <Row>
                      <Col span={12}>
                        <Button type="primary" onClick={this.onInitDeploy}>
                          申请服务器
                        </Button>
                      </Col>
                      <Col span={12}>
                        <Button type="primary" onClick={this.onDestroyInstance}>
                          销毁服务器
                        </Button> 
                      </Col>
                    </Row>
                    <Row>
                      <Col span={12}>
                        <Button type="primary" onClick={this.onDeployBrookServer}>
                          部署BROOK服务端
                        </Button>
                      </Col>
                    </Row>
                    <Row>
                      <Col span={12}>
                        <Button type="primary" onClick={this.onDeployBrookClient}>
                          部署BROOK客户端
                        </Button>
                      </Col>
                    </Row>
                  </Form.Item>
                </Form>
              </Col>
              <Col span={12}>
                <div
                  style={{
                    paddingRight: 16
                  }}
                >
                  <TextArea
                    autosize={{ minRows: 20, maxRows: 20 }}
                    style={{
                      whiteSpace: 'nowrap'
                    }}
                    value={allMsg}
                  />
                </div>
              </Col>
            </Row>
          </TabPane>
          <TabPane
            tab="配置"
            key="2"
          >
            <Row gutter={16}>
              <Col
                span={12}
              >
                <SimpleForm
                  onFormMount={(form) => {
                    this.configForm = form;
                    this.getConfig();
                  }}
                  data={this.props.config}
                  onSubmit={this.onSaveConfig}
                  config={{
                    region: {
                      label: 'Region'
                    },
                    accessKeyId: {
                      label: 'Access Key ID'
                    },
                    accessKeySecret: {
                      label: 'Access Key Secret'
                    },
                    rootPassword: {
                      label: 'ROOT 密码'
                    },
                    brookPassword: {
                      label: 'BROOK 密码'
                    },
                    brookPort: {
                      label: 'BROOK PORT'
                    }
                  }}
                />
              </Col>
            </Row>
          </TabPane>
        </Tabs>
      </div>
    );
  } 
}


export default connect(({ index }) => ({
  ...index,
}))(Index);