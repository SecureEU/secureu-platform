import DocsSidebar from '@/components/docs/DocsSidebar';
import DocsHeader from '@/components/docs/DocsHeader';
import DocsContent from '@/components/docs/DocsContent';
import ArticleContent from '@/components/docs/ArticleContent';
import { ThemeProvider } from '@/components/docs/ThemeProvider';

export default function DocsLayout({ children }) {
  return (
    <ThemeProvider>
      <DocsHeader />
      <DocsContent>
        <DocsSidebar />
        <ArticleContent>
          {children}
        </ArticleContent>
        <div className="hidden xl:block w-64 flex-shrink-0" />
      </DocsContent>
    </ThemeProvider>
  );
}
