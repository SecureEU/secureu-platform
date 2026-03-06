
import { Pie } from 'react-chartjs-2';
import {PieData}  from '../utils/types'
import { Chart as ChartJS, Title, Tooltip, Legend, ArcElement, CategoryScale, LinearScale } from 'chart.js';
import { faker } from '@faker-js/faker';

// Register necessary Chart.js components
ChartJS.register(Title, Tooltip, Legend, ArcElement, CategoryScale, LinearScale);


const DemoPie = ({data, title,}:{data: PieData[], title:string} ) => {

  const pieChartData = {
    labels: data.map(item => item.type), // Array of labels (type)
    datasets:[{
      label: title,
      data: data.map(item => item.value),
      backgroundColor: data.map(() => faker.color.rgb()),
      hoverOffset: 4,
    }
    ],
    
  }

   // Pie chart options
   const options = {
    responsive: true, // Ensures the chart resizes on window resize
    plugins: {
      title: {
        display: true,
        text: title // Title for the chart
      },
      tooltip: {
        callbacks: {
          label: (context: any) => {
            const label = context.label || '';
            const value = context.raw || 0;
            return `${label}: ${value} units`; // Format the tooltip
          }
        }
      }
    }
  };
  return <Pie  data={pieChartData} options={options} />;
};

export default DemoPie;