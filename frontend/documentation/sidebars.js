/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  tutorialSidebar: [
    {
      type: 'doc',
      id: 'intro',
      label: 'Introduction',
    },
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/installation',
        'getting-started/configuration',
        'getting-started/quick-start',
      ],
    },
    {
      type: 'category',
      label: 'Features',
      items: [
        'features/dashboard',
        'features/scans',
        'features/exploitation',
        'features/data-traffic-monitoring',
        'features/anomaly-detection',
        'features/botnet-detection',
        'features/compliance',
        'features/stix-integration',
      ],
    },
    {
      type: 'category',
      label: 'User Guide',
      items: [
        'user-guide/running-scans',
        'user-guide/managing-assets',
        'user-guide/exploitation-testing',
        'user-guide/generating-reports',
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      items: [
        'api/overview',
        'api/authentication',
        'api/endpoints',
      ],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: [
        'architecture/overview',
        'architecture/backend',
        'architecture/frontend',
        'architecture/database',
      ],
    },
  ],
};

export default sidebars;
