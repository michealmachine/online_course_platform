export function createWorker(workerFunction: () => void) {
  const blob = new Blob(
    [`(${workerFunction.toString()})()`],
    { type: 'application/javascript' }
  );
  return new Worker(URL.createObjectURL(blob));
} 