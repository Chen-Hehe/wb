import request from '../utils/request';

/**
 * 动态报表查询
 * @param prompt 自然语言需求
 * @param chartType 图表类型 (pie/bar/line)
 */
export const dynamicReport = (prompt: string, chartType: string) => {
  return request.post('/reports/dynamic', {
    prompt,
    chartType,
  });
};
