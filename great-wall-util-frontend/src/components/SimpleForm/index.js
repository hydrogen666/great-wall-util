import React from 'react';
import { Form, Input, Button } from 'antd';
import _ from 'lodash';
const formItemLayout = {
  labelCol: { span: 8 },
  wrapperCol: { span: 10 },
};
const formButtonLayout = {
  labelCol: { span: 8 },
  wrapperCol: { span: 10, offset: 8 },
};
class SimpleForm extends React.Component {

  static defaultProps = {
    config: {},
    onSubmit: () => {},
    onFormMount: () => {},
  }
  constructor(props) {
    super(props);
  }
  componentDidMount() {
    this.props.onFormMount(this.props.form);
  }
  handleSubmit = (e) => {
    this.props.form.validateFields((err, values) => {
      if (!err) {
        this.props.onSubmit(values);
      }
    });
  }
  render() {
    const { getFieldDecorator } = this.props.form;

    return (
      <Form {...formItemLayout} onSubmit={this.handleSubmit}>
        {
          _.map(this.props.config, (config, key) => {
            return (
              <Form.Item label={_.get(config, 'label', key)} key={key}>
                {getFieldDecorator(key, {
                  rules: [{ required: true }],
                })(
                  <Input />,
                )}
              </Form.Item>
            );
          })
        }
        <Form.Item {...formButtonLayout}>
          <Button type="primary" htmlType="submit">
            提交
            </Button>
        </Form.Item>
      </Form>
    )
  }
}

export default Form.create()(SimpleForm);