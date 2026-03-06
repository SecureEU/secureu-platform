// @ts-check
import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'SECUR-EU Documentation',
  tagline: 'SME Security Platform',
  favicon: 'img/favicon.ico',

  url: 'https://secur-eu.eu',
  baseUrl: '/',

  organizationName: 'inter-soc',
  projectName: 'intersoc-dashboard',

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          routeBasePath: '/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/secureu-social-card.jpg',
      navbar: {
        title: 'SECUR-EU',
        logo: {
          alt: 'SECUR-EU Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Documentation',
          },
          {
            href: 'http://localhost:3000',
            label: 'Dashboard',
            position: 'right',
          },
          {
            href: 'http://localhost:3001/docs',
            label: 'API Reference',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Documentation',
            items: [
              {
                label: 'Getting Started',
                to: '/getting-started',
              },
              {
                label: 'Features',
                to: '/features',
              },
              {
                label: 'API Reference',
                href: 'http://localhost:3001/docs',
              },
            ],
          },
          {
            title: 'Platform',
            items: [
              {
                label: 'Dashboard',
                href: 'http://localhost:3000',
              },
              {
                label: 'Scans',
                href: 'http://localhost:3000/scans',
              },
              {
                label: 'Exploitation',
                href: 'http://localhost:3000/exploitation',
              },
            ],
          },
          {
            title: 'Resources',
            items: [
              {
                label: 'GitHub',
                href: 'https://github.com/secur-eu',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} SECUR-EU. Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['bash', 'json', 'yaml', 'go'],
      },
      colorMode: {
        defaultMode: 'light',
        disableSwitch: false,
        respectPrefersColorScheme: true,
      },
    }),
};

export default config;
