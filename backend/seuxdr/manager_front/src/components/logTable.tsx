import { Table } from "antd";



const LogTable = ({ dataSource, columns }: { dataSource: any; columns: any }) => {
  return <Table dataSource={dataSource} columns={columns} />;
};

export default LogTable;
