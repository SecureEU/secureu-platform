'use client';

import { useTheme } from './ThemeProvider';

export default function ArticleContent({ children }) {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  return (
    <main className={`flex-1 p-8 max-w-4xl ${isDark ? 'bg-gray-950' : 'bg-white'}`}>
      <article className={`
        max-w-none
        ${isDark ? 'text-gray-100' : 'text-gray-900'}
        [&_h1]:text-3xl [&_h1]:font-bold [&_h1]:mb-4 [&_h1]:mt-8
        [&_h2]:text-2xl [&_h2]:font-semibold [&_h2]:mb-3 [&_h2]:mt-8
        [&_h3]:text-xl [&_h3]:font-semibold [&_h3]:mb-2 [&_h3]:mt-6
        [&_h4]:text-lg [&_h4]:font-semibold [&_h4]:mb-2 [&_h4]:mt-4
        ${isDark
          ? '[&_h1]:text-white [&_h2]:text-white [&_h3]:text-white [&_h4]:text-white'
          : '[&_h1]:text-gray-900 [&_h2]:text-gray-900 [&_h3]:text-gray-900 [&_h4]:text-gray-900'
        }
        [&_p]:mb-4 [&_p]:leading-7
        ${isDark ? '[&_p]:text-gray-200' : '[&_p]:text-gray-700'}
        [&_a]:underline
        ${isDark ? '[&_a]:text-blue-400 hover:[&_a]:text-blue-300' : '[&_a]:text-blue-600 hover:[&_a]:text-blue-700'}
        [&_strong]:font-semibold
        ${isDark ? '[&_strong]:text-white' : '[&_strong]:text-gray-900'}
        [&_ul]:list-disc [&_ul]:pl-6 [&_ul]:mb-4
        [&_ol]:list-decimal [&_ol]:pl-6 [&_ol]:mb-4
        [&_li]:mb-2
        ${isDark ? '[&_li]:text-gray-200' : '[&_li]:text-gray-700'}
        [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:rounded [&_code]:text-sm [&_code]:font-mono
        ${isDark
          ? '[&_code]:bg-gray-800 [&_code]:text-gray-200'
          : '[&_code]:bg-gray-100 [&_code]:text-gray-800'
        }
        [&_pre]:p-4 [&_pre]:rounded-lg [&_pre]:overflow-x-auto [&_pre]:mb-4
        ${isDark
          ? '[&_pre]:bg-gray-900 [&_pre]:border [&_pre]:border-gray-700'
          : '[&_pre]:bg-gray-50 [&_pre]:border [&_pre]:border-gray-200'
        }
        [&_pre_code]:p-0 [&_pre_code]:bg-transparent
        [&_blockquote]:border-l-4 [&_blockquote]:pl-4 [&_blockquote]:italic [&_blockquote]:my-4
        ${isDark
          ? '[&_blockquote]:border-gray-600 [&_blockquote]:text-gray-300'
          : '[&_blockquote]:border-gray-300 [&_blockquote]:text-gray-600'
        }
        [&_table]:w-full [&_table]:mb-4 [&_table]:border-collapse
        [&_th]:text-left [&_th]:p-2 [&_th]:font-semibold
        [&_td]:p-2
        ${isDark
          ? '[&_th]:bg-gray-800 [&_th]:border-gray-700 [&_td]:border-gray-700 [&_th]:text-white [&_td]:text-gray-200'
          : '[&_th]:bg-gray-100 [&_th]:border-gray-200 [&_td]:border-gray-200 [&_th]:text-gray-900 [&_td]:text-gray-700'
        }
        [&_th]:border [&_td]:border
        [&_hr]:my-8
        ${isDark ? '[&_hr]:border-gray-700' : '[&_hr]:border-gray-200'}
      `}>
        {children}
      </article>
    </main>
  );
}
