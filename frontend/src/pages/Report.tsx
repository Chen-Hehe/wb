import React, { useState } from 'react';
import { Card, Input, Select, Button, Space, message, Spin } from 'antd';
import ReactECharts from 'echarts-for-react';
import { SendOutlined } from '@ant-design/icons';
import { dynamicReport } from '../api/report';
import './Report.css';

const { TextArea } = Input;
const { Option } = Select;

const Report: React.FC = () => {
  const [prompt, setPrompt] = useState('');
  const [chartType, setChartType] = useState('bar');
  const [loading, setLoading] = useState(false);
  const [echartOption, setEchartOption] = useState<any>(null);

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      message.warning('请输入报表需求');
      return;
    }

    setLoading(true);
    try {
      const result: any = await dynamicReport(prompt, chartType);
      
      if (result && result.echart_option) {
        // 执行字符串代码获取 option 对象
        // eslint-disable-next-line no-new-func
        const getOption = new Function(result.echart_option + '; return option;');
        const option = getOption();
        setEchartOption(option);
        message.success('报表生成成功');
      } else {
        message.error('报表生成失败：未获取到图表配置');
      }
    } catch (error: any) {
      console.error('报表生成失败:', error);
      message.error(error.message || '报表生成失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const chartTypeOptions = [
    { value: 'bar', label: '柱状图' },
    { value: 'line', label: '折线图' },
    { value: 'pie', label: '饼图' },
  ];

  return (
    <div className="report-page">
      <Card className="report-card" title="AI 动态报表" size="small">
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          {/* 输入区域 */}
          <div className="input-section">
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
              报表需求
            </label>
            <TextArea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="请输入您的报表需求，例如：统计发微博数量最多的前 10 个用户"
              autoSize={{ minRows: 3, maxRows: 6 }}
              style={{ marginBottom: 16 }}
            />
            
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <label style={{ fontWeight: 500 }}>图表类型：</label>
              <Select
                value={chartType}
                onChange={setChartType}
                style={{ width: 150 }}
              >
                {chartTypeOptions.map((opt) => (
                  <Option key={opt.value} value={opt.value}>
                    {opt.label}
                  </Option>
                ))}
              </Select>
              
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={handleGenerate}
                loading={loading}
                size="large"
              >
                生成报表
              </Button>
            </div>
          </div>

          {/* 图表展示区域 */}
          <div className="chart-section">
            {loading ? (
              <div className="loading-container">
                <Spin size="large" tip="正在生成报表..." />
              </div>
            ) : echartOption ? (
              <ReactECharts
                option={echartOption}
                style={{ height: 400 }}
                opts={{ renderer: 'canvas' }}
              />
            ) : (
              <div className="empty-chart">
                <p style={{ color: '#999', textAlign: 'center' }}>
                  请输入报表需求并点击"生成报表"按钮
                </p>
              </div>
            )}
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default Report;
