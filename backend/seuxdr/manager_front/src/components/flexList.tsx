import { Spin, Flex, Card, Typography } from 'antd';
import { ChartData } from '../utils/types';
import { CSSProperties } from 'react';

const { Text } = Typography;

const getCardStyle = (value: number | string): CSSProperties => ({
  width: 320,
  borderRadius: 12,
  boxShadow: '0 4px 10px rgba(0, 0, 0, 0.15)',
  transition: '0.3s',
  background: typeof value === 'number' && value > 50 ? '#ff7875' : '#f0f2f5',
  color: '#333',
  textAlign: 'center' as CSSProperties['textAlign'], // Explicitly typed
});

const valueStyle: CSSProperties = {
  fontSize: '2rem', // Increased font size
  fontWeight: 700,
  color: '#1890ff', // Highlight color
  display: 'block',
  marginTop: 12,
};

const FlexList = ({ listItems }: { listItems: ChartData[] }) => {
  return (
    <Flex gap="large" vertical>
      <Flex wrap="wrap" gap="large" justify="center">
        {listItems.map((item, index) => (
          <Spin key={index} tip="Loading..." size="large" spinning={item.value === ''}>
            <Card title={<Text strong>{item.text}</Text>} style={getCardStyle(item.value)}>
              <Text style={valueStyle}>{item.value}</Text>
            </Card>
          </Spin>
        ))}
      </Flex>
    </Flex>
  );
};

export default FlexList;
