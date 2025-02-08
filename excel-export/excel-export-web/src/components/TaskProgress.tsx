import { useEffect, useState } from 'react'
import { Progress } from './Progress'

interface TaskProgressProps {
  taskId: number
  onCompleted: () => void
}

export function TaskProgress({ taskId, onCompleted }: TaskProgressProps) {
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    const eventSource = new EventSource(`/api/export/status?taskId=${taskId}`)

    // eventSource.onmessage = debounce({ delay: 100 }, ({ data }) => {
    //   setProgress(Number(data))
    //   if (data === '100') {
    //     onCompleted()
    //   }
    // })
    eventSource.onmessage = ({ data }) => {
      setProgress(Number(data))
      if (data === '100') {
        onCompleted()
      }
    }

    return () => {
      eventSource.close()
    }
  }, [onCompleted, taskId])

  return <Progress value={progress} />
}
