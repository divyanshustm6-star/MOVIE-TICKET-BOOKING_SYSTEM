import { useCallback, useEffect, useState } from 'react';

export function useAsync(loader, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const run = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await loader();
      setData(result);
      return result;
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Unable to load data');
      throw err;
    } finally {
      setLoading(false);
    }
  }, deps);

  useEffect(() => {
    run().catch(() => {});
  }, [run]);

  return { data, loading, error, reload: run, setData };
}
