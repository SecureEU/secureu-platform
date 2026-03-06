import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ChartOptions
} from 'chart.js';
import annotationPlugin from 'chartjs-plugin-annotation'; // Import annotation plugin
import { Bar } from 'react-chartjs-2';
import { StackedBarData } from '../utils/types';
import { faker } from '@faker-js/faker';

// Register plugins
ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  annotationPlugin
);

// Function to return dynamic chart options
export const createChartOptions = (title: string, color: string, min: number, max: number): ChartOptions<'bar'> => {
  return {
    plugins: {
      title: {
        display: true,
        text: title,
      },
      annotation: {
        annotations: {
          zeroLine: {
            type: 'line',
            xMin: min,
            xMax: max,
            borderColor: color, // Use color from argument
            borderWidth: 2,
          }
        }
      }
    },
    responsive: true,
    scales: {
      x: { 
        stacked: true,
        display:true,
        grid: {
          offset: true,
        },
        ticks: {
          callback: function(value) {
            if (typeof value ===  'string') {
              return value
            }
            // Access the label at the current index
            const label = this.getLabelForValue(value);
  
            // Check if the label ends with "00" using substring
            if (label && label?.includes(":00")) {

              if (label.includes("00:00")) {
                // Split the label by '00:00'
                const parts = label.split('00:00');

                // Construct the resulting array
                const result = ['00:00', parts[1].trim()];
                return result
              }
              return label; // Show label
            }
            return ''; // Hide the label for intervals not ending in "00"
          },
          autoSkip:false,
          font: (context) => {
            const width = context.chart.width;
            const size = width < 500 ? 5 : 8; // Adjust these values as needed
            return { size: size };
          },
          maxRotation: 0,
          minRotation: 0,
        }
        
       },
      y: { stacked: true,
          display:true,
       },
    },
  };
};

const StackBar = ({options, barLabels, barDatasetLabels, groupedAlerts} : {options: any, barLabels: string[],barDatasetLabels: string[], groupedAlerts:StackedBarData[]}) => {
  const data = {
    labels: barLabels,
    datasets: barDatasetLabels?.map(agent => ({
      label: agent, // Each dataset corresponds to an agent
      data: barLabels.map(interval => {
        // Adjust interval comparison for 00:00
        const alert = groupedAlerts.find(alert => {
          const alertInterval = alert.interval === "00:00" ? `00:00 \n${alert.date}` : alert.interval;
          return alert.agent === agent && alertInterval === interval;
        });
  
        return alert ? alert.count : 0; // Use count or default to 0
      }),
      backgroundColor: faker.color.rgb(), // Random color for each agent
      barPercentage: 1,
      categoryPercentage:1
    })),
  };

 return  <Bar options={options} data={data} />;
}


export default StackBar;

